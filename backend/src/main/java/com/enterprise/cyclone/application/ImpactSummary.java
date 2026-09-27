package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.RiskLevel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregate statistics over one set of exposures, used for dashboarding and for the headline lines
 * of an advisory.
 *
 * <p>The risk histogram is kept as an {@link EnumMap} copy, so it is unmodifiable and iterates in
 * {@link RiskLevel} order rather than in the randomised order of {@code Map.copyOf}. Deterministic
 * iteration keeps serialised responses and rendered advisories byte-stable across runs.
 */
public record ImpactSummary(
        int totalAssets,
        Map<RiskLevel, Integer> countByRisk,
        RiskLevel highestRisk,
        double nearestDistanceNauticalMiles,
        int peakEstimatedWindKnots) {

    public ImpactSummary {
        if (totalAssets < 1) {
            throw new IllegalArgumentException("totalAssets must be at least 1 but was " + totalAssets);
        }
        Objects.requireNonNull(countByRisk, "countByRisk must not be null");
        countByRisk = Collections.unmodifiableMap(new EnumMap<>(countByRisk));
        Objects.requireNonNull(highestRisk, "highestRisk must not be null");
        if (!Double.isFinite(nearestDistanceNauticalMiles) || nearestDistanceNauticalMiles < 0) {
            throw new IllegalArgumentException(
                    "nearestDistanceNauticalMiles must be a finite value >= 0 but was " + nearestDistanceNauticalMiles);
        }
        if (peakEstimatedWindKnots < 0) {
            throw new IllegalArgumentException(
                    "peakEstimatedWindKnots must not be negative but was " + peakEstimatedWindKnots);
        }
    }

    /**
     * Derives the summary from an already severity-ordered list of exposures.
     *
     * @param exposures the exposures to summarise, non-null and non-empty
     * @throws IllegalArgumentException if {@code exposures} is empty
     */
    public static ImpactSummary of(List<AssetExposure> exposures) {
        Objects.requireNonNull(exposures, "exposures must not be null");
        if (exposures.isEmpty()) {
            throw new IllegalArgumentException("exposures must contain at least one asset");
        }

        EnumMap<RiskLevel, Integer> counts = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            counts.put(level, 0);
        }

        RiskLevel highest = RiskLevel.LOW;
        double nearest = Double.MAX_VALUE;
        int peakWind = 0;
        for (AssetExposure exposure : exposures) {
            counts.merge(exposure.riskLevel(), 1, Integer::sum);
            if (exposure.riskLevel().atLeast(highest)) {
                highest = exposure.riskLevel();
            }
            nearest = Math.min(nearest, exposure.distanceNauticalMiles());
            peakWind = Math.max(peakWind, exposure.estimatedWindAtAsset());
        }
        return new ImpactSummary(exposures.size(), counts, highest, nearest, peakWind);
    }

    /**
     * Number of assets at {@code level}, zero when none were assessed at that level.
     */
    public int countOf(RiskLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        return countByRisk.getOrDefault(level, 0);
    }

    /**
     * Number of assets at {@link RiskLevel#HIGH} or above.
     */
    public int actionableAssets() {
        return countOf(RiskLevel.HIGH) + countOf(RiskLevel.CRITICAL);
    }
}
