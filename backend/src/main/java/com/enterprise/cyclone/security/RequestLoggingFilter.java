package com.enterprise.cyclone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Emits one structured access log line per request, after the response is committed.
 *
 * <p>Only the method, path, status, duration and principal are recorded. Request and response bodies
 * are never logged, and neither is any header, because both carry credentials: a bearer token in
 * {@code Authorization} and a password in the token request body. The correlation id reaches the
 * line through the MDC.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            LOG.info("{} {} -> {} in {} ms (principal={})",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMillis,
                    principalName());
        }
    }

    private static String principalName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
