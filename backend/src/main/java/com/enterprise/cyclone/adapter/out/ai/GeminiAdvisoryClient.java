package com.enterprise.cyclone.adapter.out.ai;

import java.net.URI;
import java.util.List;

/**
 * Contract for the multimodal model gateway used to write advisories.
 *
 * <p>The model receives an instruction plus imagery references; it never receives domain objects.
 * That boundary is deliberate: prompt construction belongs to the adapter that implements
 * {@link com.enterprise.cyclone.application.port.AdvisoryGenerator}, which extracts the facts it
 * needs from
 * {@link com.enterprise.cyclone.application.AdvisoryContext}. The domain therefore never depends on
 * a prompt format, a model name or a vendor SDK, and the deterministic advisory stays available as
 * the fallback whenever this gateway is absent, slow or degraded.
 *
 * <p>Implementations must be thread-safe, must bound their own latency, and must not return null or
 * blank text. A failed generation must raise an unchecked exception rather than return partial
 * prose, because an advisory that silently degrades to silence is worse than one that is not sent.
 */
public interface GeminiAdvisoryClient {

    /**
     * Produces advisory prose from an instruction and any satellite imagery to reason over.
     *
     * @param instruction the fully rendered prompt, non-null and non-blank
     * @param imageryUris references to imagery accompanying the instruction, non-null, possibly
     *        empty; elements must be non-null
     * @return non-blank advisory prose
     * @throws IllegalArgumentException if {@code instruction} is blank or an imagery reference is null
     * @throws IllegalStateException if the model cannot be reached or returns nothing usable
     */
    String reason(String instruction, List<URI> imageryUris);
}
