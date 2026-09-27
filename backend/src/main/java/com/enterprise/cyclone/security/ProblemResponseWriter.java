package com.enterprise.cyclone.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.enterprise.cyclone.web.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Writes RFC 9457 problem responses from places that are outside the controller advice: servlet
 * filters, the authentication entry point and the access-denied handler.
 *
 * <p>Every response carries the correlation id so that a client report maps to one log line, and no
 * response ever contains a stack trace or an internal class name.
 */
@Component
public class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialises a problem document and commits the response.
     */
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String title,
            String detail) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        String correlationId = CorrelationId.current(request);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
            response.setHeader(CorrelationId.HEADER, correlationId);
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
