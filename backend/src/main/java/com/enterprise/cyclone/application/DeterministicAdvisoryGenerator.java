package com.enterprise.cyclone.application;

import com.enterprise.cyclone.application.port.AdvisoryGenerator;
import com.enterprise.cyclone.application.port.LocalizedAdvisoryGenerator;
import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.RiskLevel;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Template-based {@link AdvisoryGenerator} that needs no external service.
 *
 * <p>It is the default wiring and the fallback for the model-backed generator: it is pure,
 * deterministic and fast enough to run inside a request, and it states the model's own limits in
 * the text it emits so that an operator never mistakes a screening score for a forecast.
 *
 * <p>It is also the reason a model outage cannot silence a warning. When the language model is
 * unreachable, times out, or returns text that fails the domain checks in
 * {@link com.enterprise.cyclone.adapter.out.ai.GeminiAdvisoryGenerator}, this generator produces the
 * advisory instead and {@link AdvisoryProvenance} records that the fallback ran.
 *
 * <p>Wording comes from {@link AdvisoryVocabulary}; per-asset rule citations come from the domain's
 * own rationale and therefore remain in English in every language, which the README states.
 */
public final class DeterministicAdvisoryGenerator implements AdvisoryGenerator, LocalizedAdvisoryGenerator {

    /** Assets named individually in the advisory; the rest are summarised by count. */
    public static final int MAX_LISTED_ASSETS = 5;

    @Override
    public String generate(AdvisoryContext context) {
        return generate(context, AdvisoryLanguage.EN);
    }

    @Override
    public String generate(AdvisoryContext context, AdvisoryLanguage language) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(language, "language must not be null");

        StringBuilder advisory = new StringBuilder(1024);
        appendHeader(advisory, context, language);
        appendSummary(advisory, context, language);
        appendPriorityAssets(advisory, context.exposures(), language);
        advisory.append(AdvisoryVocabulary.basis(language));
        return advisory.toString();
    }

    private static void appendHeader(StringBuilder advisory, AdvisoryContext context, AdvisoryLanguage language) {
        CycloneTrackPoint storm = context.stormPosition();
        advisory.append(AdvisoryVocabulary.title(context.stormId(), language)).append('\n')
                .append(AdvisoryVocabulary.validAt(context.evaluatedAt(), language)).append('\n')
                .append(AdvisoryVocabulary.centre(storm, language)).append('\n')
                .append(AdvisoryVocabulary.intensity(storm, context.category(), language)).append('\n');
    }

    private static void appendSummary(StringBuilder advisory, AdvisoryContext context, AdvisoryLanguage language) {
        ImpactSummary summary = context.summary();
        advisory.append(AdvisoryVocabulary.assetsAssessed(summary.totalAssets(), language)).append('\n')
                .append(AdvisoryVocabulary.actionRequired(advisoryActionCount(context), language)).append('\n');

        RiskLevel[] levels = RiskLevel.values();
        for (int index = levels.length - 1; index >= 0; index--) {
            RiskLevel level = levels[index];
            advisory.append(AdvisoryVocabulary.countLine(level, summary.countOf(level), language)).append('\n');
        }
        advisory.append(AdvisoryVocabulary.nearestAsset(
                        summary.nearestDistanceNauticalMiles(), summary.peakEstimatedWindKnots(), language))
                .append('\n');
    }

    /**
     * The number of assets needing pre-landfall action.
     *
     * <p>Computed from the exposures rather than read from the summary so that the advisory cannot
     * disagree with the table it accompanies if the summary ever changes shape.
     */
    private static int advisoryActionCount(AdvisoryContext context) {
        return (int) context.exposures().stream().filter(AssetExposure::isActionable).count();
    }

    private static void appendPriorityAssets(
            StringBuilder advisory, List<AssetExposure> exposures, AdvisoryLanguage language) {
        advisory.append(AdvisoryVocabulary.priorityAssets(language)).append('\n');

        int listed = Math.min(exposures.size(), MAX_LISTED_ASSETS);
        for (int index = 0; index < listed; index++) {
            AssetExposure exposure = exposures.get(index);
            // Explicit newline rather than %n: an advisory is a published document, and its bytes must
            // not depend on the platform that rendered it.
            advisory.append(String.format(
                    Locale.ROOT,
                    "  %d. [%s] %s (%s) - %.0f nm, %d kt: %s\n",
                    index + 1,
                    AdvisoryVocabulary.riskName(exposure.riskLevel(), language),
                    exposure.asset().name(),
                    exposure.asset().assetType(),
                    exposure.distanceNauticalMiles(),
                    exposure.estimatedWindAtAsset(),
                    exposure.rationale()));
        }
        if (exposures.size() > listed) {
            advisory.append(AdvisoryVocabulary.moreAssets(exposures.size() - listed, language)).append('\n');
        }
    }
}
