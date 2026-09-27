package com.enterprise.cyclone.application;

import com.enterprise.cyclone.application.port.LocalizedAdvisoryGenerator;
import java.util.Objects;

/**
 * Chooses how an advisory gets written, and records which path ran.
 *
 * <p>This is the whole reliability story of model-backed advisories in one class. A language model
 * is attempted only when it is configured; if it fails, times out, or returns text that fails the
 * domain checks, the deterministic generator produces the advisory and the provenance says so. The
 * caller — and therefore the operator reading the screen — can always tell the difference, and no
 * configuration error can turn a warning into an error page.
 *
 * <p>Failures are caught rather than propagated on purpose: the cost of a wrong advisory is high,
 * but the cost of no advisory during a cyclone is higher.
 *
 * <p>Both generators are language-aware. That matters more than it looks: the fallback is not an
 * English-only escape hatch, it is the path an operator in Andhra Pradesh actually reads when a
 * model call fails, so a Telugu request must fall back to a Telugu advisory.
 */
public final class AdvisoryComposer {

    private final LocalizedAdvisoryGenerator fallback;
    private final LocalizedAdvisoryGenerator modelGenerator;
    private final boolean modelEnabled;
    private final boolean modelRequired;
    private final String modelName;

    /**
     * @param fallback the always-available template generator, non-null and language-aware
     * @param modelGenerator the model-backed generator, or null when none is configured
     * @param modelEnabled whether the model may be used for this deployment
     * @param modelRequired whether the configuration explicitly demands the model, in which case a
     *        missing configuration is reported as a degraded start rather than a normal choice
     * @param modelName the model identifier recorded in provenance, non-blank
     */
    public AdvisoryComposer(
            LocalizedAdvisoryGenerator fallback,
            LocalizedAdvisoryGenerator modelGenerator,
            boolean modelEnabled,
            boolean modelRequired,
            String modelName) {
        this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
        this.modelGenerator = modelGenerator;
        this.modelEnabled = modelEnabled && modelGenerator != null;
        this.modelRequired = modelRequired;
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName must not be blank");
        }
        this.modelName = modelName.strip();
    }

    /**
     * Produces the advisory for one assessment.
     *
     * @param context the assessment facts, non-null
     * @param language the language to publish in, non-null
     * @return the advisory and how it was produced, never null
     */
    public AdvisoryOutcome compose(AdvisoryContext context, AdvisoryLanguage language) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(language, "language must not be null");

        if (!modelEnabled) {
            long startedAt = System.nanoTime();
            String text = fallback.generate(context, language);
            long elapsed = elapsedMillis(startedAt);

            // A deployment that demanded the model and did not get it is degraded, even though the
            // call was never attempted: an operator asked for model-written advisories and is not
            // receiving them. A deployment that simply has no model is a normal configuration.
            return new AdvisoryOutcome(
                    text,
                    modelRequired
                            ? AdvisoryProvenance.fallback(
                                    language,
                                    elapsed,
                                    "the language model was requested (app.ai.advisory.mode=gemini) but is "
                                            + "not configured, so the deterministic generator produced this advisory")
                            : AdvisoryProvenance.deterministic(
                                    language,
                                    elapsed,
                                    "no language model is configured, so the deterministic generator "
                                            + "produced this advisory"));
        }

        long startedAt = System.nanoTime();
        try {
            String text = modelGenerator.generate(context, language);
            if (text == null || text.isBlank()) {
                // The port forbids this, so a blank return is a contract violation, not a valid
                // outcome. Treating it as failure keeps a broken adapter from silencing a warning.
                throw new IllegalStateException("the advisory generator returned no text");
            }
            return new AdvisoryOutcome(
                    text, AdvisoryProvenance.model(modelName, language, elapsedMillis(startedAt)));
        } catch (RuntimeException failure) {
            String text = fallback.generate(context, language);
            return new AdvisoryOutcome(
                    text, AdvisoryProvenance.fallback(language, elapsedMillis(startedAt), describe(failure)));
        }
    }

    /** True when a language model is actually configured and available for use. */
    public boolean modelEnabled() {
        return modelEnabled;
    }

    /** The model identifier recorded in provenance. */
    public String modelName() {
        return modelName;
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    /**
     * A one-line, non-sensitive description of a model failure.
     *
     * <p>Exception class plus message: enough to tell a timeout from a rejected payload from a
     * blocked response, and deliberately not the whole stack trace, because this text is rendered in
     * the console and copied into chats.
     */
    static String describe(Throwable failure) {
        String message = failure.getMessage();
        String reason = message == null || message.isBlank() ? "no further detail" : message.strip();
        if (reason.length() > 240) {
            reason = reason.substring(0, 240) + "…";
        }
        return failure.getClass().getSimpleName() + ": " + reason;
    }
}
