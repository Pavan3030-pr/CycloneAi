package com.enterprise.cyclone.application.port;

import com.enterprise.cyclone.application.AdvisoryContext;
import com.enterprise.cyclone.application.AdvisoryLanguage;

/**
 * Port for advisory generators that can be told which language to write in.
 *
 * <p>Kept separate from {@link AdvisoryGenerator} so that the existing single-language contract —
 * and everything wired to it, including the domain self-test — keeps working unchanged. A generator
 * that can only write English implements that port; anything that can follow an instruction
 * implements this one.
 *
 * <p>Implementations must bound their own latency, must be thread-safe, and must signal failure by
 * raising an unchecked exception rather than by returning null or blank text. The caller decides
 * what to do about a failure; this port never degrades quietly, because a silently substituted
 * advisory is exactly the failure mode provenance exists to prevent.
 */
@FunctionalInterface
public interface LocalizedAdvisoryGenerator {

    /**
     * Renders the advisory for one assessment in the requested language.
     *
     * @param context the fact sheet of the assessment, non-null
     * @param language the language to write in, non-null
     * @return non-blank advisory text
     * @throws NullPointerException if any argument is null
     * @throws IllegalStateException if generation fails or the returned text is unusable
     */
    String generate(AdvisoryContext context, AdvisoryLanguage language);
}
