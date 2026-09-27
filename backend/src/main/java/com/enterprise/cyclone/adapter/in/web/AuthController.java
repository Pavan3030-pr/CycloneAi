package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exchanges a credential pair for a bearer token.
 *
 * <p>This is the only anonymous endpoint that accepts input. A failure returns one generic message
 * for both an unknown username and a wrong password, so the endpoint cannot be used to enumerate
 * accounts, and it is behind the rate limiter so it cannot be used to grind passwords either.
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final JwtTokenService jwtTokenService;

    public AuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Operation(summary = "Issue a bearer token for a valid credential pair")
    @SecurityRequirements
    @PostMapping(path = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return jwtTokenService.issue(request.username(), request.password())
                .map(issued -> new TokenResponse(issued.value(), "Bearer", issued.expiresAt(), issued.roles()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }

    /**
     * Credentials presented for a token.
     *
     * @param username the configured login name, at most 64 characters
     * @param password the password, at most 256 characters
     */
    public record TokenRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 256) String password) {
    }

    /**
     * An issued token.
     *
     * @param accessToken the signed JWT to send as {@code Authorization: Bearer <token>}
     * @param tokenType always {@code Bearer}
     * @param expiresAt the instant the token stops being accepted
     * @param roles the roles the token carries
     */
    public record TokenResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            List<String> roles) {
    }
}
