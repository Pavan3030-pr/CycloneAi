package com.enterprise.cyclone.security;

import com.enterprise.cyclone.web.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes one correlation id per request and clears it afterwards.
 *
 * <p>An inbound {@code X-Correlation-Id} is accepted only when it matches a conservative character
 * class and length; anything else is replaced with a generated value. That check matters because
 * the value is written straight into every log line, so an attacker-supplied newline could
 * otherwise forge log entries.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationIdOf(request);
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        request.setAttribute(CorrelationId.ATTRIBUTE, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private static String correlationIdOf(HttpServletRequest request) {
        String supplied = request.getHeader(CorrelationId.HEADER);
        return supplied != null && SAFE_ID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
    }
}
