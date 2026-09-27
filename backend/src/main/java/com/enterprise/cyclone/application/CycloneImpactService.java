package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.service.ExposureCalculator;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The primary use case: assess the impact of one storm on one set of assets and publish an advisory.
 *
 * <p>The orchestrator holds no state beyond its collaborators and performs no I/O, which keeps the
 * use case trivially testable and safe to call concurrently. It depends on interfaces only, so the
 * exposure model and the advisory wording can each be replaced without touching this sequence.
 *
 * <p>Advisory wording goes through {@link AdvisoryComposer} rather than straight to a generator, so
 * that this class never has to know whether a language model or a template produced the text — only
 * that the text and its provenance come back together.
 */
public final class CycloneImpactService {

    private final ExposureCalculator exposureCalculator;
    private final AdvisoryComposer advisoryComposer;

    public CycloneImpactService(ExposureCalculator exposureCalculator, AdvisoryComposer advisoryComposer) {
        this.exposureCalculator = Objects.requireNonNull(exposureCalculator, "exposureCalculator must not be null");
        this.advisoryComposer = Objects.requireNonNull(advisoryComposer, "advisoryComposer must not be null");
    }

    /**
     * Assesses the storm at its latest known fix, the usual choice when a caller has no explicit
     * evaluation time. The advisory is written in English.
     *
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code assets} is empty
     */
    public ImpactReport assess(CycloneTrack track, Collection<InfrastructureAsset> assets) {
        Objects.requireNonNull(track, "track must not be null");
        return assess(track, assets, track.endTime(), AdvisoryLanguage.EN);
    }

    /**
     * Assesses the storm at {@code evaluationTime}, writing the advisory in English.
     *
     * @param track the storm timeline, non-null
     * @param assets the assets to assess, non-null and non-empty
     * @param evaluationTime the instant to evaluate, non-null and within the track's timeline
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code assets} is empty or {@code evaluationTime} lies
     *         outside the timeline
     */
    public ImpactReport assess(CycloneTrack track, Collection<InfrastructureAsset> assets, Instant evaluationTime) {
        return assess(track, assets, evaluationTime, AdvisoryLanguage.EN);
    }

    /**
     * Assesses the storm at {@code evaluationTime} and writes the advisory in {@code language}.
     *
     * @param track the storm timeline, non-null
     * @param assets the assets to assess, non-null and non-empty
     * @param evaluationTime the instant to evaluate, non-null and within the track's timeline
     * @param language the language to publish the advisory in, non-null
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code assets} is empty or {@code evaluationTime} lies
     *         outside the timeline
     */
    public ImpactReport assess(
            CycloneTrack track,
            Collection<InfrastructureAsset> assets,
            Instant evaluationTime,
            AdvisoryLanguage language) {
        Objects.requireNonNull(track, "track must not be null");
        Objects.requireNonNull(assets, "assets must not be null");
        Objects.requireNonNull(evaluationTime, "evaluationTime must not be null");
        Objects.requireNonNull(language, "language must not be null");

        CycloneTrackPoint stormPosition = track.interpolateAt(evaluationTime);
        List<AssetExposure> exposures = exposureCalculator.calculate(track, assets, evaluationTime);
        ImpactSummary summary = ImpactSummary.of(exposures);
        AdvisoryContext context =
                new AdvisoryContext(track.stormId(), evaluationTime, stormPosition, summary, exposures);

        AdvisoryOutcome advisory = advisoryComposer.compose(context, language);

        return new ImpactReport(
                track.stormId(),
                evaluationTime,
                stormPosition,
                summary,
                advisory.text(),
                advisory.provenance(),
                exposures);
    }
}
