package com.enterprise.cyclone.adapter.out.notify;

import com.enterprise.cyclone.application.port.AlertDispatcher;
import com.enterprise.cyclone.application.port.DispatchRequest;
import com.enterprise.cyclone.application.port.DispatchResult;
import com.enterprise.cyclone.config.DisseminationProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Publishes advisories to a JSON webhook.
 *
 * <p>One HTTP adapter covers the whole dissemination story because that is how these integrations are
 * actually built: an SMS gateway, a WhatsApp Business provider, a Telegram bot and a state emergency
 * operations centre all accept a JSON POST, and which one sits behind the URL is deployment
 * configuration rather than application code. A Cloud Function that fans out to several channels is
 * a legitimate value for the same setting.
 *
 * <p>Dispatch is best-effort by design. The advisory has already been generated and rendered before
 * this call; a messaging provider having a bad minute must not fail the assessment that produced a
 * warning, so a failure is returned as a result and shown to the operator rather than thrown.
 */
public final class RestWebhookAlertDispatcher implements AlertDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(RestWebhookAlertDispatcher.class);

    /** Channel name reported back to the caller and shown in the console. */
    public static final String CHANNEL = "webhook";

    private static final int MAXIMUM_DETAIL_CHARACTERS = 240;

    private final RestClient client;
    private final DisseminationProperties.Webhook webhook;

    public RestWebhookAlertDispatcher(RestClient client, DisseminationProperties.Webhook webhook) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.webhook = Objects.requireNonNull(webhook, "webhook must not be null");
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public boolean configured() {
        return webhook.configured();
    }

    @Override
    public DispatchResult dispatch(DispatchRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        if (!configured()) {
            return DispatchResult.failed(
                    CHANNEL,
                    "no notification channel is configured on this deployment "
                            + "(set app.dissemination.webhook.url to enable dispatch)",
                    0L);
        }

        long startedAt = System.nanoTime();
        try {
            ResponseEntity<String> response = client.post()
                    .uri(webhook.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (webhook.token() != null) {
                            headers.setBearerAuth(webhook.token());
                        }
                    })
                    .body(payload(request))
                    .retrieve()
                    .toEntity(String.class);

            long elapsed = elapsedMillis(startedAt);
            LOG.info("Dispatched advisory for storm {} to {} in {} ms with status {}",
                    request.stormId(), webhook.url(), elapsed, response.getStatusCode().value());
            return DispatchResult.delivered(
                    CHANNEL,
                    "accepted by the notification channel with HTTP " + response.getStatusCode().value(),
                    elapsed);
        } catch (RestClientResponseException rejection) {
            return DispatchResult.failed(
                    CHANNEL,
                    "the notification channel rejected the advisory with HTTP "
                            + rejection.getStatusCode().value() + shortBody(rejection),
                    elapsedMillis(startedAt));
        } catch (RestClientException transportFailure) {
            return DispatchResult.failed(
                    CHANNEL,
                    "the notification channel could not be reached: " + shorten(transportFailure.getMessage()),
                    elapsedMillis(startedAt));
        }
    }

    /**
     * The wire payload.
     *
     * <p>Field names are written for the receiving integrator rather than for this codebase: a gateway
     * needs a message to send, a control room needs the headline and the identifiers, and anything
     * that understands CAP gets the standards document alongside.
     */
    private Map<String, Object> payload(DispatchRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", CHANNEL);
        payload.put("source", "cycloneai");
        payload.put("stormId", request.stormId());
        payload.put("language", request.language().code());
        payload.put("headline", request.headline());
        payload.put("message", request.message());
        payload.put("advisory", request.advisory());
        payload.put("capXml", request.capXml());
        return payload;
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private static String shortBody(RestClientResponseException rejection) {
        String body = rejection.getResponseBodyAsString();
        return body == null || body.isBlank() ? "" : ": " + shorten(body.replaceAll("\\s+", " ").strip());
    }

    private static String shorten(String value) {
        if (value == null) {
            return "no further detail";
        }
        String trimmed = value.strip();
        return trimmed.length() > MAXIMUM_DETAIL_CHARACTERS
                ? trimmed.substring(0, MAXIMUM_DETAIL_CHARACTERS) + "…"
                : trimmed;
    }
}
