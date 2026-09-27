package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The result of one impact assessment: what the storm looked like, what it means for each asset,
 * the aggregate picture, and the advisory text to publish.
 *
 * <p>Every field is derived from the supplied track and assets at one instant, so a report is a
 * reproducible snapshot rather than a live view. Persisting it later is an adapter concern; nothing
 * in this class knows about storage or transport.
 *
 * <p>The advisory travels with its {@link AdvisoryProvenance} because a published warning has to
 * carry the answer to "who wrote this, and did the model fail first?" — that answer cannot be
 * reconstructed later from the text alone.
 */
public record ImpactReport(
        String stormId,
        Instant evaluatedAt,
        CycloneTrackPoint stormPosition,
        ImpactSummary summary,
        String advisory,
        AdvisoryProvenance advisoryProvenance,
        List<AssetExposure> exposures) {

    public ImpactReport {
        if (stormId == null || stormId.isBlank()) {
            throw new IllegalArgumentException("stormId must not be blank");
        }
        stormId = stormId.strip();
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        Objects.requireNonNull(stormPosition, "stormPosition must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        if (advisory == null || advisory.isBlank()) {
            throw new IllegalArgumentException("advisory must not be blank");
        }
        Objects.requireNonNull(advisoryProvenance, "advisoryProvenance must not be null");
        Objects.requireNonNull(exposures, "exposures must not be null");
        if (exposures.isEmpty()) {
            throw new IllegalArgumentException("exposures must contain at least one asset");
        }
        exposures = List.copyOf(exposures);
    }

    /**
     * The assets needing pre-landfall action, most exposed first.
     */
    public List<AssetExposure> actionableExposures() {
        return exposures.stream().filter(AssetExposure::isActionable).toList();
    }

    /**
     * The language this report's advisory was issued in.
     */
    public AdvisoryLanguage advisoryLanguage() {
        return advisoryProvenance.language();
    }
}
