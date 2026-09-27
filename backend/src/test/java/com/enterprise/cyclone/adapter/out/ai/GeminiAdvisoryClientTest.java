package com.enterprise.cyclone.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The Gemini adapter, tested without a network.
 *
 * <p>The HTTP contract is asserted rather than assumed: the model has to be in the path, the key in
 * the header, and the facts in the prompt, because every one of those has a silent failure mode. A
 * missing key header produces a 401 that looks like a bad key; a prompt missing the numbers produces
 * a fluent, confident, wrong advisory.
 *
 * <p>Failures are asserted to become {@link IllegalStateException} with a short message, which is
 * what lets the composer fall back and record why in provenance.
 */
class GeminiAdvisoryClientTest {

    private static final String API_KEY = "test-key-not-a-real-secret";
    private static final String MODEL = "gemini-2.5-flash";

    private MockRestServiceServer server;
    private RestGeminiAdvisoryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.invalid/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestGeminiAdvisoryClient(builder.build(), API_KEY, MODEL, 900, 0.2d);
    }

    @Test
    void sendsTheModelInThePathTheKeyInTheHeaderAndTheInstructionInTheBody() {
        server.expect(requestTo("https://generativelanguage.invalid/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("RENDERED PROMPT"))
                .andExpect(jsonPath("$.generationConfig.temperature").value(0.2d))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(900))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").isNotEmpty())
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"content":{"parts":[{"text":"ADVISORY TEXT"}]},"finishReason":"STOP"}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String text = client.reason("RENDERED PROMPT", List.of());

        assertThat(text).isEqualTo("ADVISORY TEXT");
        server.verify();
    }

    @Test
    void concatenatesMultiPartResponses() {
        server.expect(requestTo("https://generativelanguage.invalid/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"content":{"parts":[{"text":"PART ONE "},{"text":"PART TWO"}]}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThat(client.reason("PROMPT", List.of())).isEqualTo("PART ONE PART TWO");
    }

    @Test
    void passesImageryReferencesAsTextRatherThanFetchingThem() {
        server.expect(requestTo("https://generativelanguage.invalid/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(jsonPath("$.contents[0].parts[1].text")
                        .value("Available imagery reference: gs://bucket/scene.tif"))
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"content":{"parts":[{"text":"ADVISORY"}]}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThat(client.reason("PROMPT", List.of(URI.create("gs://bucket/scene.tif")))).isEqualTo("ADVISORY");
    }

    @Test
    void reportsAnHttpRejectionWithItsStatusAndBody() {
        server.expect(requestTo("https://generativelanguage.invalid/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"message\":\"Quota exceeded\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> client.reason("PROMPT", List.of()))
                .withMessageContaining("HTTP 429")
                .withMessageContaining("Quota exceeded");
    }

    @Test
    void reportsABlockedPromptRatherThanReturningNothing() {
        server.expect(requestTo("https://generativelanguage.invalid/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withSuccess(
                        """
                        {"promptFeedback":{"blockReason":"SAFETY"}}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> client.reason("PROMPT", List.of()))
                .withMessageContaining("blocked")
                .withMessageContaining("SAFETY");
    }

    @Test
    void reportsACandidateCutShortWithNoText() {
        server.expect(requestTo("https://generativelanguage.invalid/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"content":{"parts":[]},"finishReason":"MAX_TOKENS"}]}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> client.reason("PROMPT", List.of()))
                .withMessageContaining("MAX_TOKENS");
    }

    @Test
    void rejectsAnEmptyInstructionBeforeCallingAnything() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> client.reason("  ", List.of()))
                .withMessageContaining("instruction");
    }

    @Test
    void requiresAnApiKey() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RestGeminiAdvisoryClient(RestClient.create(), null, MODEL, 900, 0.2d))
                .withMessageContaining("apiKey");
    }
}
