package com.enterprise.cyclone.application;

import java.util.Locale;
import java.util.Optional;

/**
 * Languages an advisory can be issued in.
 *
 * <p>The model and the template generator both take one of these, and the CAP writer emits the code
 * as the alert's {@code <language>} element, so one value drives the whole publication path. The
 * beta codes are deliberately languages of the Indian coastline rather than a generic list: an
 * advisory that a district officer cannot read in the language they brief in does not get used.
 *
 * <p>Display names are in the language itself, because a selector that offers "Telugu" to a Telugu
 * speaker is a selector that was designed for the developer.
 */
public enum AdvisoryLanguage {

    /** English. */
    EN("en", "English"),

    /** Hindi. */
    HI("hi", "हिन्दी"),

    /** Telugu — the language of the Andhra Pradesh and Telangana coast, where the demonstration
     * storm makes landfall. */
    TE("te", "తెలుగు");

    private final String code;
    private final String displayName;

    AdvisoryLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /** ISO 639-1 code, also used as the CAP {@code <language>} value. */
    public String code() {
        return code;
    }

    /** Name of the language in that language. */
    public String displayName() {
        return displayName;
    }

    /** Parses a language code case-insensitively. */
    public static Optional<AdvisoryLanguage> parse(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalised = code.strip().toLowerCase(Locale.ROOT);
        // Tolerate a regional tag such as en-IN or te-IN by matching on the primary subtag.
        String primary = normalised.contains("-") ? normalised.substring(0, normalised.indexOf('-')) : normalised;
        for (AdvisoryLanguage language : values()) {
            if (language.code.equals(normalised) || language.code.equals(primary)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    /**
     * Parses a language code, falling back to {@link #EN} for anything unrecognised or absent.
     *
     * <p>Lenient on purpose: an advisory in a language we do not support is better delivered in
     * English than not delivered, and the response states which language was actually used.
     */
    public static AdvisoryLanguage fromCode(String code) {
        return parse(code).orElse(EN);
    }
}
