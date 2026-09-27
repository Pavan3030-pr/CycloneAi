package com.enterprise.cyclone.application.port;

import com.enterprise.cyclone.application.AdvisoryContext;

/**
 * Port for turning an impact assessment into human-readable early-warning text.
 *
 * <p>The domain knows nothing about this port: exposure and risk are computed in
 * {@code domain.service}, and advisory wording is an application concern that may later be served
 * by a multimodal model instead of a template.
 *
 * <p>Implementations must be thread-safe and must not mutate the supplied context. A remote
 * implementation must bound its own latency and, on failure, either return its best available text
 * or raise an unchecked exception &mdash; it must never return null or blank text, because an empty
 * advisory silently suppresses a warning.
 */
@FunctionalInterface
public interface AdvisoryGenerator {

    /**
     * Renders the advisory for one assessment.
     *
     * @param context the fact sheet of the assessment, non-null
     * @return non-blank advisory text
     * @throws NullPointerException if {@code context} is null
     */
    String generate(AdvisoryContext context);
}
