package com.enterprise.cyclone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects oversized request bodies before they are read.
 *
 * <p>The declared {@code Content-Length} is checked first, which costs nothing and stops a large
 * body at the door. A chunked request declares no length, so the parser-level limits configured on
 * the Jackson factory bound its nesting depth, string length and number length instead; between
 * them, no request can make the server allocate an unbounded structure.
 */
public class PayloadSizeLimitFilter extends OncePerRequestFilter {

    private final long maxPayloadBytes;
    private final ProblemResponseWriter problemWriter;

    public PayloadSizeLimitFilter(SecurityProperties properties, ProblemResponseWriter problemWriter) {
        this.maxPayloadBytes = properties.maxPayloadBytes();
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long declaredLength = request.getContentLengthLong();
        if (declaredLength > maxPayloadBytes) {
            problemWriter.write(request, response, HttpStatus.PAYLOAD_TOO_LARGE, "Payload too large",
                    "Request body must not exceed " + maxPayloadBytes + " bytes but declared " + declaredLength);
            return;
        }
        chain.doFilter(request, response);
    }
}
