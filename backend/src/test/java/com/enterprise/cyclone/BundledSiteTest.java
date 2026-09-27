package com.enterprise.cyclone;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
 * The single-origin deployment shape: console and API served by the same service.
 *
 * <p>This is the path a reviewer actually uses — one URL, no CORS, relative {@code /api} calls — and
 * it is the easiest thing in the repository to break silently, because a permission added for the
 * site can quietly open the API with it. Both halves are therefore asserted together: the shell and
 * every client-side route are reachable without a token, and the API is not.
 *
 * <p>The site itself is supplied by a stub in the test resources. The production image puts the real
 * build in the same place, so what is under test is the wiring: the route forwards, the
 * static-resource permissions, and the fact that anything unrecognised still falls through to
 * {@code authenticated()} rather than to a wildcard permit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BundledSiteTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void shellAndClientRoutesAreServedWithoutAToken() {
        for (String path : List.of("/", "/index.html", "/signin", "/signup", "/app", "/app/advisory", "/app/assessment")) {
            ResponseEntity<String> response = browserGet(path);

            assertThat(response.getStatusCode()).as("%s is served", path).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getContentType()).as("%s is html", path)
                    .isNotNull()
                    .satisfies(type -> assertThat(type.toString()).contains("text/html"));
            assertThat(response.getBody()).as("%s returns the shell", path).contains("id=\"root\"");
        }
    }

    @Test
    void servingTheSiteDoesNotOpenTheApi() {
        // The console is public; the data behind it is not. Without a token the asset registry must
        // still answer 401 even though the page that calls it is now served from this origin.
        ResponseEntity<String> assets = browserGet("/api/v1/assets");
        assertThat(assets.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // A mistyped API path must not be swallowed by a site-wide catch-all and answered with HTML.
        ResponseEntity<String> unknown = browserGet("/api/v1/does-not-exist");
        assertThat(unknown.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);

        // Actuator stays where it was: the health probe is public, the rest needs an administrator.
        assertThat(browserGet("/actuator/health").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(browserGet("/actuator/beans").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void securityHeadersStillCoverTheShell() {
        HttpHeaders headers = browserGet("/").getHeaders();

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Content-Security-Policy"))
                .as("the console needs its typefaces and map tiles, and nothing else")
                .contains("default-src 'self'")
                .contains("https://fonts.googleapis.com")
                .contains("https://fonts.gstatic.com")
                .contains("https://*.tile.openstreetmap.org")
                .contains("frame-ancestors 'none'");
    }

    /**
     * A browser navigation: it asks for HTML, which is what makes the resource handler serve the
     * shell rather than answer 406.
     */
    private ResponseEntity<String> browserGet(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.ALL));
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
