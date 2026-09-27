package com.enterprise.cyclone.application.port;

import com.enterprise.cyclone.application.AdvisoryLanguage;
import java.util.Objects;

/**
 * One advisory on its way out of the building.
 *
 * <p>Carries both the plain text and, when it was produced, the CAP document — because the two
 * consumers are different: a messaging gateway wants a short readable message, an emergency
 * operations centre wants the standards document.
 *
 * @param stormId the storm the advisory belongs to, non-blank
 * @param language the language the text is written in, non-null
 * @param headline the one-line summary, non-blank
 * @param advisory the full advisory text, non-blank
 * @param capXml the CAP 1.2 document, or null when CAP was not requested
 */
public record DispatchRequest(
        String stormId,
        AdvisoryLanguage language,
        String headline,
        String advisory,
        String capXml) {

    public DispatchRequest {
        if (stormId == null || stormId.isBlank()) {
            throw new IllegalArgumentException("stormId must not be blank");
        }
        Objects.requireNonNull(language, "language must not be null");
        if (headline == null || headline.isBlank()) {
            throw new IllegalArgumentException("headline must not be blank");
        }
        if (advisory == null || advisory.isBlank()) {
            throw new IllegalArgumentException("advisory must not be blank");
        }
    }

    /** The message body a messaging gateway would send, headline first. */
    public String message() {
        return headline + "\n\n" + advisory;
    }
}
