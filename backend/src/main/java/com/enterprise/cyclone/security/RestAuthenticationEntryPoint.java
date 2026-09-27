package com.enterprise.cyclone.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Answers unauthenticated calls with a problem document instead of a redirect to a login page.
 *
 * <p>It also logs the refusal at WARN with the reason class only. The reason is never returned to
 * the caller: telling a client whether its token was expired, malformed or signed with the wrong key
 * helps an attacker and helps nobody else.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final ProblemResponseWriter problemWriter;

    public RestAuthenticationEntryPoint(ProblemResponseWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        LOG.warn("Rejected unauthenticated request to {} ({})",
                request.getRequestURI(), authenticationException.getClass().getSimpleName());
        problemWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Authentication required",
                "Supply a valid bearer token obtained from POST /api/v1/auth/token");
    }
}
