package com.enterprise.cyclone.domain.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * The predicted impact of a cyclone on one infrastructure asset at one evaluation instant.
 *
 * <p>This is the unit of currency for the whole forecaster: the exposure calculator produces it,
 * the impact report aggregates it, and advisories and downstream clients consume it. It carries its
 * own {@code rationale} so that every score in a disaster-management audit trail can be explained
 * without reconstructing the inputs.
 */
public record AssetExposure(
        InfrastructureAsset asset,
        double distanceNauticalMiles,
        int estimatedWindAtAsset,
        RiskLevel riskLevel,
        String rationale) {

    /**
     * Most severe first, then nearest first. Ordering is part of the model rather than a caller
     * concern so that every consumer ranks assets consistently.
     */
    public static final Comparator<AssetExposure> BY_SEVERITY =
            Comparator.comparing(AssetExposure::riskLevel, Comparator.reverseOrder())
                    .thenComparingDouble(AssetExposure::distanceNauticalMiles);

    public AssetExposure {
        Objects.requireNonNull(asset, "asset must not be null");
        if (!Double.isFinite(distanceNauticalMiles) || distanceNauticalMiles < 0) {
            throw new IllegalArgumentException(
                    "distanceNauticalMiles must be a finite value >= 0 but was " + distanceNauticalMiles);
        }
        if (estimatedWindAtAsset < CycloneTrackPoint.MIN_WIND_SPEED_KNOTS
                || estimatedWindAtAsset > CycloneTrackPoint.MAX_WIND_SPEED_KNOTS) {
            throw new IllegalArgumentException("estimatedWindAtAsset must be within ["
                    + CycloneTrackPoint.MIN_WIND_SPEED_KNOTS + ", " + CycloneTrackPoint.MAX_WIND_SPEED_KNOTS
                    + "] knots but was " + estimatedWindAtAsset);
        }
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        rationale = rationale.strip();
    }

    /**
     * Whether this exposure warrants evacuation or hardening decisions before landfall.
     */
    public boolean isActionable() {
        return riskLevel.atLeast(RiskLevel.HIGH);
    }
}
