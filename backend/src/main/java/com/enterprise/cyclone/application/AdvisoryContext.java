package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The complete, self-contained fact sheet of one assessment.
 *
 * <p>This is the input to every {@link com.enterprise.cyclone.application.port.AdvisoryGenerator}.
 * It exists as an explicit type so that a template-based generator and a future multimodal model
 * are fed the same structured facts, and so that no advisory implementation has to reach back into
 * the calculator, the clock or the request to understand what happened.
 */
public record AdvisoryContext(
        String stormId,
        Instant evaluatedAt,
        CycloneTrackPoint stormPosition,
        ImpactSummary summary,
        List<AssetExposure> exposures) {

    public AdvisoryContext {
        if (stormId == null || stormId.isBlank()) {
            throw new IllegalArgumentException("stormId must not be blank");
        }
        stormId = stormId.strip();
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        Objects.requireNonNull(stormPosition, "stormPosition must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(exposures, "exposures must not be null");
        if (exposures.isEmpty()) {
            throw new IllegalArgumentException("exposures must contain at least one asset");
        }
        exposures = List.copyOf(exposures);
    }

    /**
     * Intensity of the storm at {@link #evaluatedAt()}.
     */
    public SaffirSimpsonCategory category() {
        return SaffirSimpsonCategory.fromWindSpeedKnots(stormPosition.windSpeedKnots());
    }
}
