package com.enterprise.cyclone.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Gemini client speaking the {@code generateContent} REST API.
 *
 * <p>Implemented against the documented HTTP contract rather than a vendor SDK, for three reasons
 * that all matter in an operations context: the dependency surface stays small enough to audit, the
 * endpoint is configuration rather than code (so the same adapter can point at the Gemini Developer
 * API, at Vertex AI, or at an internal gateway that brokers the key), and the request shape is
 * visible in one file where a reviewer can read exactly what leaves the building.
 *
 * <p>What leaves the building is a rendered prompt: a handful of numbers from the assessment and a
 * short list of asset names with their rule citations. No credentials, no asset coordinates beyond
 * the storm centre, and no operator identity, because an advisory only needs the facts it quotes.
 *
 * <p>Imagery references are passed as text. Sending actual pixels would mean fetching objects from
 * storage inside a request thread, which trades a bounded, explainable call for an unbounded one;
 * that fetch belongs in a background export, not in the path that writes a warning.
 *
 * <p>Failures are converted into {@link IllegalStateException} with a short, non-sensitive message so
 * the caller can fall back and record why.
 */
public final class RestGeminiAdvisoryClient implements GeminiAdvisoryClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestGeminiAdvisoryClient.class);

    /**
     * The model's role, held here rather than in the domain so that changing the house style of an
     * advisory never touches a class under {@code domain} or {@code application}.
     */
    private static final String SYSTEM_INSTRUCTION = """
            You are the duty forecaster of a coastal cyclone warning centre. You write short, calm, \
            operational impact advisories for district control rooms: consequence first, numbers exact, \
            no speculation. You never invent facts. If a fact is not supplied to you, you do not mention it.""";

    private static final int MAX_ERROR_DETAIL_CHARACTERS = 240;

    /** Header the Gemini Developer API expects for a key; Vertex AI uses a bearer token instead. */
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;
    private final double temperature;

    public RestGeminiAdvisoryClient(
            RestClient client, String apiKey, String model, int maxOutputTokens, double temperature) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        if (apiKey == null || apiKey.isBlank()) {
            // Constructed only when the configuration has a key, so reaching here means a wiring bug
            // rather than a deployment without one.
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        this.apiKey = apiKey.strip();
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        this.model = model.strip();
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    /** The model this client calls, as recorded in advisory provenance. */
    public String model() {
        return model;
    }

    @Override
    public String reason(String instruction, List<URI> imageryUris) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be blank");
        }
        Objects.requireNonNull(imageryUris, "imageryUris must not be null");
        for (URI uri : imageryUris) {
            Objects.requireNonNull(uri, "imagery references must not be null");
        }

        JsonNode response;
        try {
            response = client.post()
                    .uri("/models/{model}:generateContent", model)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(instruction, imageryUris))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException rejection) {
            throw new IllegalStateException(
                    "Gemini rejected the request with HTTP " + rejection.getStatusCode().value()
                            + describeProblem(rejection),
                    rejection);
        } catch (RestClientException transportFailure) {
            throw new IllegalStateException("Gemini could not be reached: " + transportFailure.getMessage(),
                    transportFailure);
        }

        if (response == null) {
            throw new IllegalStateException("Gemini returned an empty response body");
        }
        String text = extractText(response);
        LOG.debug("Gemini produced {} characters using model {}", text.length(), model);
        return text;
    }

    private Map<String, Object> requestBody(String instruction, List<URI> imageryUris) {
        List<Map<String, Object>> parts = new ArrayList<>(1 + imageryUris.size());
        parts.add(Map.of("text", instruction));
        for (URI imagery : imageryUris) {
            // Named as references so the model can mention that imagery exists without pretending to
            // have analysed pixels it was never given.
            parts.add(Map.of("text", "Available imagery reference: " + imagery));
        }

        return Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens));
    }

    /**
     * Pulls the text out of a {@code generateContent} response.
     *
     * <p>A prompt that safety filters block, or a completion cut short by the token ceiling before any
     * text is produced, must be reported as a failure rather than returned as an empty advisory — an
     * empty warning is indistinguishable from an all-clear.
     */
    private static String extractText(JsonNode response) {
        JsonNode blockReason = response.path("promptFeedback").path("blockReason");
        if (!blockReason.isMissingNode() && !blockReason.isNull() && !blockReason.asText().isBlank()) {
            throw new IllegalStateException("Gemini blocked the prompt: " + blockReason.asText());
        }

        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates");
        }

        JsonNode first = candidates.get(0);
        StringBuilder text = new StringBuilder();
        for (JsonNode part : first.path("content").path("parts")) {
            JsonNode partText = part.path("text");
            if (partText.isTextual()) {
                text.append(partText.asText());
            }
        }

        String candidate = text.toString().strip();
        if (candidate.isEmpty()) {
            String finishReason = first.path("finishReason").asText("");
            throw new IllegalStateException(finishReason.isBlank()
                    ? "Gemini returned a candidate with no text"
                    : "Gemini returned no text (finish reason " + finishReason + ")");
        }
        return candidate;
    }

    /** A short, non-sensitive summary of an error response, for the provenance detail. */
    private static String describeProblem(RestClientResponseException rejection) {
        String body = rejection.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "";
        }
        String detail = body.replaceAll("\\s+", " ").strip();
        if (detail.length() > MAX_ERROR_DETAIL_CHARACTERS) {
            detail = detail.substring(0, MAX_ERROR_DETAIL_CHARACTERS) + "…";
        }
        return ": " + detail;
    }
}
