package com.enterprise.cyclone.adapter.out.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.enterprise.cyclone.application.AdvisoryLanguage;
import com.enterprise.cyclone.application.port.DispatchRequest;
import com.enterprise.cyclone.application.port.DispatchResult;
import com.enterprise.cyclone.config.DisseminationProperties;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The outbound notification path.
 *
 * <p>A dispatch is only useful if the operator can trust its answer, so the tests cover both states
 * plainly: an unconfigured deployment says it has no channel rather than reporting success, and a
 * provider that answers with an error is reported as a failure with the status the provider returned
 * instead of being swallowed.
 */
class RestWebhookAlertDispatcherTest {

    private static final String ENDPOINT = "https://alerts.invalid/dispatch";

    @Test
    void reportsThatNoChannelIsConfiguredRatherThanPretendingToSend() {
        RestWebhookAlertDispatcher dispatcher = new RestWebhookAlertDispatcher(
                RestClient.create(), new DisseminationProperties.Webhook(false, null, null, null, null));

        assertThat(dispatcher.configured()).isFalse();
        assertThat(dispatcher.channel()).isEqualTo("webhook");

        DispatchResult result = dispatcher.dispatch(request());

        assertThat(result.delivered()).isFalse();
        assertThat(result.detail()).contains("no notification channel is configured");
        assertThat(result.channel()).isEqualTo("webhook");
    }

    @Test
    void postsTheAdvisoryWithABearerTokenAndReportsAcceptance() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestWebhookAlertDispatcher dispatcher = new RestWebhookAlertDispatcher(
                builder.build(), new DisseminationProperties.Webhook(true, ENDPOINT, "secret-token", null, null));

        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(jsonPath("$.stormId").value("IO-DEMO-01"))
                .andExpect(jsonPath("$.language").value("hi"))
                .andExpect(jsonPath("$.headline").value("चक्रवात IO-DEMO-01 ..."))
                .andExpect(jsonPath("$.advisory").value("ADVISORY BODY"))
                .andExpect(jsonPath("$.message").value("चक्रवात IO-DEMO-01 ...\n\nADVISORY BODY"))
                .andExpect(jsonPath("$.capXml").isNotEmpty())
                .andRespond(withSuccess("{\"queued\":true}", MediaType.APPLICATION_JSON));

        DispatchResult result = dispatcher.dispatch(request());

        assertThat(result.delivered()).isTrue();
        assertThat(result.detail()).contains("HTTP 200");
        server.verify();
    }

    @Test
    void reportsAProviderRejectionWithItsStatus() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestWebhookAlertDispatcher dispatcher = new RestWebhookAlertDispatcher(
                builder.build(), new DisseminationProperties.Webhook(true, ENDPOINT, null, null, null));

        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("gateway down"));

        DispatchResult result = dispatcher.dispatch(request());

        assertThat(result.delivered()).isFalse();
        assertThat(result.detail()).contains("HTTP 502").contains("gateway down");
    }

    @Test
    void reportsAnUnreachableProviderWithoutThrowing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestWebhookAlertDispatcher dispatcher = new RestWebhookAlertDispatcher(
                builder.build(), new DisseminationProperties.Webhook(true, ENDPOINT, null, null, null));

        server.expect(requestTo(ENDPOINT)).andRespond(request -> {
            throw new IOException("connection refused");
        });

        DispatchResult result = dispatcher.dispatch(request());

        assertThat(result.delivered()).isFalse();
        assertThat(result.detail()).contains("could not be reached");
    }

    @Test
    void reportsTheChannelConfiguredForReadiness() {
        DisseminationProperties properties = new DisseminationProperties(
                "ops@example.gov.in",
                "State EOC",
                Duration.ofHours(12),
                new DisseminationProperties.Webhook(true, ENDPOINT, null, null, null));

        assertThat(properties.webhook().configured()).isTrue();
        assertThat(properties.validity()).isEqualTo(Duration.ofHours(12));
        assertThat(properties.senderName()).isEqualTo("State EOC");
    }

    @Test
    void enabledWithoutAUrlIsStillNotConfigured() {
        DisseminationProperties properties = new DisseminationProperties(
                null, null, null, new DisseminationProperties.Webhook(true, "  ", null, null, null));

        assertThat(properties.webhook().configured()).isFalse();
        assertThat(properties.sender()).isEqualTo("cycloneai@localhost");
        assertThat(properties.validity()).isEqualTo(Duration.ofHours(24));
    }

    private static DispatchRequest request() {
        return new DispatchRequest(
                "IO-DEMO-01",
                AdvisoryLanguage.HI,
                "चक्रवात IO-DEMO-01 ...",
                "ADVISORY BODY",
                "<alert/>");
    }
}
