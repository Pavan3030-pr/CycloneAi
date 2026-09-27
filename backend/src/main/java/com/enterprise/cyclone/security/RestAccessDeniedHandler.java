package com.enterprise.cyclone.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Answers authenticated-but-unauthorised calls with a problem document.
 *
 * <p>The response names the required capability in general terms rather than echoing the caller's
 * actual roles, which keeps the API from confirming what a compromised account is allowed to do.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ProblemResponseWriter problemWriter;

    public RestAccessDeniedHandler(ProblemResponseWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        LOG.warn("Denied request to {} for the authenticated principal", request.getRequestURI());
        problemWriter.write(request, response, HttpStatus.FORBIDDEN, "Insufficient role",
                "The authenticated principal lacks the role required for this operation");
    }
}
