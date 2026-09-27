package com.enterprise.cyclone.application;

import java.util.Objects;

/**
 * Where an advisory actually came from.
 *
 * <p>This is the difference between a demo and an operational system. An operator reading an
 * advisory has to know whether a language model wrote it or a deterministic template did, which
 * model, how long it took, and whether the model was asked and failed. Without that, a fallback is
 * indistinguishable from a success, and nobody can explain after the fact why two advisories for the
 * same storm read differently.
 *
 * @param generator the strategy that produced the text: {@code gemini} or {@code deterministic}
 * @param model the model identifier, or null when not model-generated
 * @param language the language the text was issued in
 * @param latencyMillis wall-clock time spent producing the text
 * @param degraded true when a model was attempted and failed, so the fallback ran
 * @param detail human-readable explanation: why the fallback ran, or why no model was used
 */
public record AdvisoryProvenance(
        String generator,
        String model,
        AdvisoryLanguage language,
        long latencyMillis,
        boolean degraded,
        String detail) {

    /** Generator name for template output. */
    public static final String DETERMINISTIC = "deterministic";

    /** Generator name for model output. */
    public static final String GEMINI = "gemini";

    public AdvisoryProvenance {
        if (generator == null || generator.isBlank()) {
            throw new IllegalArgumentException("generator must not be blank");
        }
        generator = generator.strip();
        Objects.requireNonNull(language, "language must not be null");
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis must not be negative");
        }
        if (detail != null && detail.isBlank()) {
            detail = null;
        }
    }

    /** True when the text was written by a language model rather than a template. */
    public boolean modelGenerated() {
        return GEMINI.equals(generator) && !degraded;
    }

    public static AdvisoryProvenance deterministic(AdvisoryLanguage language, long latencyMillis, String detail) {
        return new AdvisoryProvenance(DETERMINISTIC, null, language, latencyMillis, false, detail);
    }

    public static AdvisoryProvenance fallback(AdvisoryLanguage language, long latencyMillis, String detail) {
        return new AdvisoryProvenance(DETERMINISTIC, null, language, latencyMillis, true, detail);
    }

    public static AdvisoryProvenance model(
            String model, AdvisoryLanguage language, long latencyMillis) {
        return new AdvisoryProvenance(GEMINI, model, language, latencyMillis, false, null);
    }
}
