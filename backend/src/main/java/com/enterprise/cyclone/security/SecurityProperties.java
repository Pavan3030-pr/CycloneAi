package com.enterprise.cyclone.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalised security configuration, bound from {@code app.security.*} in
 * {@code application.yml} and overridable through environment variables.
 *
 * <p>No secret is compiled into the application: {@code jwtSecret} defaults to blank, and a random
 * key is generated at startup when it is absent. That keeps local runs working without shipping a
 * signing key that would also be the production key.
 *
 * @param issuer the {@code iss} claim written into and expected of every token
 * @param jwtSecret the HS256 signing secret, at least 32 bytes; blank means "generate per boot"
 * @param tokenTtl how long a freshly minted token stays valid
 * @param apiDocsPublic whether Swagger UI and the OpenAPI document are reachable without a token
 * @param allowedOrigins browser origins permitted by CORS, exact match, no wildcards
 * @param maxPayloadBytes largest accepted request body, enforced before the body is read
 * @param users the credential directory; each password may be plaintext or a BCrypt hash
 * @param rateLimit the request budget applied to the guarded paths
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotNull String issuer,
        String jwtSecret,
        @NotNull Duration tokenTtl,
        boolean apiDocsPublic,
        @NotEmpty List<String> allowedOrigins,
        @Min(1024) long maxPayloadBytes,
        @NotEmpty List<ConfiguredUser> users,
        @NotNull RateLimit rateLimit) {

    /**
     * One configured principal.
     *
     * @param username the login name
     * @param password plaintext (encoded at startup) or a BCrypt hash beginning with {@code $2}
     * @param roles authority names without the {@code ROLE_} prefix, for example {@code ADMIN}
     */
    public record ConfiguredUser(String username, String password, List<String> roles) {
    }

    /**
     * Token-bucket budget shared by every request to a guarded path.
     *
     * @param enabled whether the limiter runs at all
     * @param capacity the burst size, that is, how many requests may arrive back to back
     * @param refillPeriod how long it takes to refill an empty bucket
     * @param paths the Ant-style path patterns the limiter guards
     */
    public record RateLimit(
            boolean enabled,
            @Min(1) int capacity,
            @NotNull Duration refillPeriod,
            @NotEmpty List<String> paths) {
    }
}
