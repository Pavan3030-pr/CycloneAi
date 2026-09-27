package com.enterprise.cyclone.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed bearer tokens for verified credentials.
 *
 * <p>Tokens are short-lived, carry the issuer so that a token minted for another environment is
 * rejected here, and carry roles in a {@code roles} claim that the resource server maps to
 * authorities. The signing key never leaves this process and is never written to a log.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final ConfiguredUserDirectory userDirectory;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, ConfiguredUserDirectory userDirectory, SecurityProperties properties,
            Clock clock) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder must not be null");
        this.userDirectory = Objects.requireNonNull(userDirectory, "userDirectory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Verifies a credential pair and mints a token for it.
     *
     * @return the issued token, or empty for any credential failure
     */
    public Optional<IssuedToken> issue(String username, String password) {
        return userDirectory.authenticate(username, password).map(this::mint);
    }

    private IssuedToken mint(ConfiguredUserDirectory.AuthenticatedPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(principal.username())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", principal.roles())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt, principal.roles());
    }

    /**
     * A freshly minted bearer token and the roles it carries.
     */
    public record IssuedToken(String value, Instant expiresAt, List<String> roles) {

        public IssuedToken {
            roles = List.copyOf(roles);
        }
    }
}
