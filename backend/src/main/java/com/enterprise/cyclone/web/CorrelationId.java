package com.enterprise.cyclone.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Correlation id contract shared by the filter that establishes it and everything that reports it.
 *
 * <p>The value is carried in the SLF4J MDC key {@link #MDC_KEY} and echoed in the
 * {@link #HEADER} response header, so a client can quote one identifier that appears on every log
 * line of its request.
 */
public final class CorrelationId {

    /** Response header carrying the correlation id. */
    public static final String HEADER = "X-Correlation-Id";

    /** MDC key under which the correlation id is available to the logging pattern. */
    public static final String MDC_KEY = "correlationId";

    /** Request attribute holding the validated correlation id. */
    public static final String ATTRIBUTE = CorrelationId.class.getName();

    private CorrelationId() {
    }

    /**
     * The correlation id of the current request, or null when the establishing filter has not run.
     */
    public static String current(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        return value instanceof String correlationId ? correlationId : null;
    }
}
