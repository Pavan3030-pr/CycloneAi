package com.enterprise.cyclone;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The API as a running service, not as a set of units.
 *
 * <p>Everything asserted here was previously only verifiable by hand with curl, which meant the
 * security policy and the advisory contract could regress without a failing build. The suite runs the
 * whole stack — filters, security chain, validation, exposure model, advisory composition, CAP
 * rendering — so the things a reviewer cares about most (authorisation, the risk split, the
 * provenance of the advisory) are properties of the build rather than claims in a document.
 *
 * <p>Two of these tests are deliberately about absence of configuration: with no Gemini key in the
 * environment, the assessment must still succeed and must say the deterministic generator wrote the
 * text. That is the graceful-degradation guarantee, and it is asserted rather than assumed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {

    private static final String ANALYST = "analyst";
    private static final String ANALYST_PASSWORD = "cyclone-demo-analyst";
    private static final String VIEWER = "viewer";
    private static final String VIEWER_PASSWORD = "cyclone-demo-viewer";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void anonymousRequestsAreRejectedWithAProblemDocument() throws Exception {
        ResponseEntity<String> response = get("/api/v1/assets", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        JsonNode problem = json(response);
        assertThat(problem.path("status").asInt()).isEqualTo(401);
        assertThat(problem.path("title").asText()).isNotBlank();
        assertThat(problem.path("correlationId").asText()).isNotBlank();
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void credentialsAreExchangedForATokenAndBadCredentialsAreNotDiagnosed() throws Exception {
        JsonNode token = json(post("/api/v1/auth/token",
                Map.of("username", ANALYST, "password", ANALYST_PASSWORD), null));

        assertThat(token.path("accessToken").asText()).isNotBlank();
        assertThat(token.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(token.path("roles").toString()).contains("ANALYST");

        ResponseEntity<String> rejected = post("/api/v1/auth/token",
                Map.of("username", ANALYST, "password", "definitely-wrong"), null);

        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rejected.getBody()).as("the response must not reveal which half was wrong")
                .contains("Invalid username or password");
    }

    @Test
    void assessingTheDemonstrationStormReturnsTheVerifiedRiskSplit() throws Exception {
        String token = analystToken();

        JsonNode assessment = json(post("/api/v1/impact-assessments", demoRequest("en"), token));

        assertThat(assessment.path("stormId").asText()).isEqualTo("IO-DEMO-01");
        assertThat(assessment.path("stormPosition").path("windSpeedKnots").asInt())
                .as("the mid-interval instant interpolates to 48 kt")
                .isEqualTo(48);
        assertThat(assessment.path("stormPosition").path("centralPressureMb").asInt()).isEqualTo(994);
        assertThat(assessment.path("stormPosition").path("category").asText()).isEqualTo("TS");

        JsonNode counts = assessment.path("summary").path("countByRisk");
        assertThat(counts.path("CRITICAL").asInt())
                .as("the shelter 13 nm from the centre trips the hurricane-force band")
                .isEqualTo(1);
        assertThat(counts.path("HIGH").asInt()).isZero();
        assertThat(counts.path("LOW").asInt()).isEqualTo(1);
        assertThat(assessment.path("summary").path("highestRisk").asText()).isEqualTo("CRITICAL");
        assertThat(assessment.path("summary").path("nearestDistanceNauticalMiles").asDouble())
                .isCloseTo(13.0, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void advisoryProvenanceIsReportedAndSaysThatNoModelIsConfigured() throws Exception {
        JsonNode assessment = json(post("/api/v1/impact-assessments", demoRequest("en"), analystToken()));

        JsonNode provenance = assessment.path("advisoryProvenance");
        assertThat(provenance.path("generator").asText())
                .as("no Gemini key is configured in the test environment")
                .isEqualTo("deterministic");
        assertThat(provenance.path("degraded").asBoolean()).isFalse();
        assertThat(provenance.path("language").asText()).isEqualTo("en");
        assertThat(provenance.path("detail").asText()).contains("no language model is configured");
        assertThat(assessment.path("advisory").asText()).startsWith("CYCLONE IMPACT ADVISORY - IO-DEMO-01");
    }

    @Test
    void theAdvisoryCanBeIssuedInHindi() throws Exception {
        JsonNode assessment = json(post("/api/v1/impact-assessments", demoRequest("hi"), analystToken()));

        assertThat(assessment.path("advisoryProvenance").path("language").asText()).isEqualTo("hi");
        assertThat(assessment.path("advisory").asText())
                .startsWith("चक्रवात प्रभाव परामर्श - IO-DEMO-01")
                .contains("48 kt");
    }

    @Test
    void anUnsupportedLanguageIsARecoverableValidationError() throws Exception {
        ResponseEntity<String> response = post("/api/v1/impact-assessments", demoRequest("fr"), analystToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("language");
    }

    @Test
    void capAlertIsAvailableAsAStandardsDocument() throws Exception {
        ResponseEntity<String> response = post("/api/v1/impact-assessments/cap", demoRequest("en"), analystToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("cap+xml");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("cycloneai-IO-DEMO-01-en-").endsWith(".xml\"");

        String document = response.getBody();
        assertThat(document)
                .contains("urn:oasis:names:tc:emergency:cap:1.2")
                .contains("<severity>Extreme</severity>")
                .contains("<urgency>Immediate</urgency>")
                .contains("<certainty>Likely</certainty>")
                .contains("<circle>15.7500,80.3000 222.24</circle>")
                .contains("<language>en</language>")
                .contains("IO-DEMO-01");
    }

    @Test
    void capAlertIsLocalisedOnRequest() throws Exception {
        ResponseEntity<String> response = post("/api/v1/impact-assessments/cap", demoRequest("te"), analystToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("<language>te</language>")
                .contains("ఉష్ణమండల తుఫాను");
    }

    @Test
    void viewerMayReadTheChannelButMayNotWriteAssetsOrDispatch() throws Exception {
        String token = tokenFor(VIEWER, VIEWER_PASSWORD);

        ResponseEntity<String> channel = get("/api/v1/advisories/channel", token);
        assertThat(channel.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(channel).path("configured").asBoolean()).isFalse();
        assertThat(json(channel).path("channel").asText()).isEqualTo("webhook");

        ResponseEntity<String> write = post("/api/v1/assets", Map.of(
                "id", "TEST-1", "name", "Test asset", "assetType", "POWER_GRID",
                "latitude", 15.0, "longitude", 80.0), token);
        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> dispatch = post("/api/v1/advisories/dispatch", demoRequest("en"), token);
        assertThat(dispatch.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void analystMayReadAssetsAndDispatchReportsAnUnconfiguredChannelHonestly() throws Exception {
        String token = analystToken();

        assertThat(get("/api/v1/assets", token).getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode dispatch = json(post("/api/v1/advisories/dispatch", demoRequest("en"), token));

        assertThat(dispatch.path("delivered").asBoolean()).isFalse();
        assertThat(dispatch.path("channelConfigured").asBoolean()).isFalse();
        assertThat(dispatch.path("detail").asText()).contains("no notification channel is configured");
        assertThat(dispatch.path("capIdentifier").asText()).startsWith("cycloneai-IO-DEMO-01-en-");
        assertThat(dispatch.path("advisory").asText()).startsWith("CYCLONE IMPACT ADVISORY");
        assertThat(dispatch.path("headline").asText()).contains("IO-DEMO-01");
    }

    @Test
    void assetWritesAndDeletesRoundTripForAnAnalyst() throws Exception {
        String token = analystToken();
        Map<String, Object> asset = Map.of(
                "id", "IT-SHELTER-1", "name", "Integration shelter", "assetType", "MEDICAL_SHELTER",
                "latitude", 15.9, "longitude", 80.47);

        assertThat(post("/api/v1/assets", asset, token).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(get("/api/v1/assets", token).getBody()).contains("IT-SHELTER-1");
        assertThat(delete("/api/v1/assets/IT-SHELTER-1", token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(delete("/api/v1/assets/IT-SHELTER-1", token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidAssessmentsAreRejectedWithFieldLevelDetail() throws Exception {
        ResponseEntity<String> response = post("/api/v1/impact-assessments", Map.of(
                "track", Map.of("stormId", "IO-DEMO-01", "points", List.of(Map.of(
                        "latitude", 15.0, "longitude", 80.0, "windSpeedKnots", 50, "centralPressureMb", 990,
                        "timestamp", "2023-12-05T00:00:00Z"))),
                "assets", List.of()), analystToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("assets");
        assertThat(response.getBody()).contains("\"errors\"");
    }

    @Test
    void healthProbeIsAnonymousAndUp() throws Exception {
        ResponseEntity<String> response = get("/actuator/health", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response).path("status").asText()).isEqualTo("UP");
    }

    private String analystToken() throws Exception {
        return tokenFor(ANALYST, ANALYST_PASSWORD);
    }

    private String tokenFor(String username, String password) throws Exception {
        JsonNode token = json(post("/api/v1/auth/token", Map.of("username", username, "password", password), null));
        String accessToken = token.path("accessToken").asText();
        assertThat(accessToken).as("token for %s", username).isNotBlank();
        return accessToken;
    }

    /**
     * The demonstration scenario: six fixes from the southern Bay of Bengal to landfall near Bapatla,
     * evaluated at the mid-interval instant the console uses, against a shelter inside the
     * hurricane-force band and a highway span far outside it.
     */
    private static Map<String, Object> demoRequest(String language) {
        return Map.of(
                "track", Map.of("stormId", "IO-DEMO-01", "points", List.of(
                        Map.of("latitude", 8.4, "longitude", 87.1, "windSpeedKnots", 30, "centralPressureMb", 1004,
                                "timestamp", "2023-12-01T00:00:00Z"),
                        Map.of("latitude", 10.3, "longitude", 85.4, "windSpeedKnots", 40, "centralPressureMb", 998,
                                "timestamp", "2023-12-02T00:00:00Z"),
                        Map.of("latitude", 12.1, "longitude", 83.6, "windSpeedKnots", 50, "centralPressureMb", 992,
                                "timestamp", "2023-12-03T00:00:00Z"),
                        Map.of("latitude", 13.9, "longitude", 81.8, "windSpeedKnots", 60, "centralPressureMb", 986,
                                "timestamp", "2023-12-04T00:00:00Z"),
                        Map.of("latitude", 15.4, "longitude", 80.5, "windSpeedKnots", 55, "centralPressureMb", 990,
                                "timestamp", "2023-12-05T00:00:00Z"),
                        Map.of("latitude", 16.1, "longitude", 80.1, "windSpeedKnots", 40, "centralPressureMb", 998,
                                "timestamp", "2023-12-05T12:00:00Z"))),
                "assets", List.of(
                        Map.of("id", "DEMO-SHELTER-BAPATLA", "name", "Bapatla coastal shelter",
                                "assetType", "MEDICAL_SHELTER", "latitude", 15.9, "longitude", 80.47),
                        Map.of("id", "DEMO-ROAD-CHENNAI", "name", "Chennai bypass (NH-32)",
                                "assetType", "ARTERIAL_ROAD", "latitude", 12.9, "longitude", 80.15)),
                "evaluationTime", "2023-12-05T06:00:00Z",
                "language", language);
    }

    private ResponseEntity<String> get(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, entity(null, token), String.class);
    }

    private ResponseEntity<String> post(String path, Object body, String token) {
        return rest.exchange(path, HttpMethod.POST, entity(body, token), String.class);
    }

    private ResponseEntity<String> delete(String path, String token) {
        return rest.exchange(path, HttpMethod.DELETE, entity(null, token), String.class);
    }

    private static HttpEntity<Object> entity(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/cap+xml")));
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        assertThat(response.getBody()).as("response body").isNotNull();
        return objectMapper.readTree(response.getBody());
    }
}
