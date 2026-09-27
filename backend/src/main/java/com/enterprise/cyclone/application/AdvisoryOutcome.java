package com.enterprise.cyclone.application;

import java.util.Objects;

/**
 * Advisory text together with its provenance.
 *
 * <p>They travel as one value because they are only meaningful together: text with no attribution
 * cannot be trusted, and attribution with no text is a log line.
 *
 * @param text the advisory as it will be published, non-blank
 * @param provenance which generator produced it and how long it took
 */
public record AdvisoryOutcome(String text, AdvisoryProvenance provenance) {

    public AdvisoryOutcome {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("advisory text must not be blank");
        }
        Objects.requireNonNull(provenance, "provenance must not be null");
    }
}
