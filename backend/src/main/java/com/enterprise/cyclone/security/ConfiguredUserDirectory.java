package com.enterprise.cyclone.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Credential directory built from {@code app.security.users}.
 *
 * <p>Passwords configured in plaintext are hashed with BCrypt once, at startup, so the running
 * application never holds a usable plaintext credential and every comparison goes through the same
 * encoder. A configured value that already looks like a BCrypt hash is used as-is, which lets a
 * deployment supply pre-hashed credentials from a secret store.
 *
 * <p>Unknown usernames still perform one hash comparison against a generated dummy, so the response
 * time of a failed login does not reveal whether a username exists.
 */
@Component
public class ConfiguredUserDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguredUserDirectory.class);
    private static final String BCRYPT_PREFIX = "$2";

    private final PasswordEncoder passwordEncoder;
    private final Map<String, Credential> credentials;
    private final String dummyPasswordHash;

    public ConfiguredUserDirectory(SecurityProperties properties, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");

        Map<String, Credential> directory = new LinkedHashMap<>();
        for (SecurityProperties.ConfiguredUser user : properties.users()) {
            String username = requireText(user.username(), "app.security.users[].username");
            String password = requireText(user.password(), "app.security.users[].password");
            directory.put(username, new Credential(encode(password), normalizeRoles(user.roles(), username)));
        }
        this.credentials = Map.copyOf(directory);
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());

        LOG.warn("Loaded {} configured principal(s) {}. Demo credentials are for local use only: override "
                + "app.security.users and set APP_JWT_SECRET before any shared deployment.",
                credentials.size(), credentials.keySet());
    }

    /**
     * Verifies a credential pair.
     *
     * @return the authenticated principal, or empty when the username is unknown or the password is
     *         wrong; the caller must not distinguish the two when reporting the failure
     */
    public Optional<AuthenticatedPrincipal> authenticate(String username, String password) {
        if (username == null || password == null) {
            passwordEncoder.matches("", dummyPasswordHash);
            return Optional.empty();
        }

        Credential credential = credentials.get(username);
        if (credential == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            return Optional.empty();
        }
        if (!passwordEncoder.matches(password, credential.encodedPassword())) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedPrincipal(username, credential.roles()));
    }

    private String encode(String configuredPassword) {
        return configuredPassword.startsWith(BCRYPT_PREFIX)
                ? configuredPassword
                : passwordEncoder.encode(configuredPassword);
    }

    private static List<String> normalizeRoles(List<String> roles, String username) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalStateException("app.security.users roles must not be empty for '" + username + "'");
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.strip().toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", ""))
                .filter(role -> !role.isEmpty())
                .distinct()
                .toList();
    }

    private static String requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must not be blank");
        }
        return value.strip();
    }

    private record Credential(String encodedPassword, List<String> roles) {
    }

    /**
     * A verified principal and the roles granted to it.
     */
    public record AuthenticatedPrincipal(String username, List<String> roles) {

        public AuthenticatedPrincipal {
            roles = List.copyOf(roles);
        }
    }
}
