package com.enterprise.cyclone.domain.service;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Domain port for turning a storm timeline into per-asset exposure.
 *
 * <p>Implementations must be pure and deterministic: the same track, assets and instant must always
 * produce the same exposures, in the same order, with no I/O, no clock reads and no shared mutable
 * state. That makes an assessment reproducible after the fact and lets a model swap (for example a
 * future wind-field or satellite-informed calculator) be validated against the existing one.
 */
public interface ExposureCalculator {

    /**
     * Assesses every asset against the storm state at {@code atTime}.
     *
     * <p>The storm state is taken from {@link CycloneTrack#interpolateAt(Instant)}, never from the
     * nearest fix, so a mid-interval evaluation is compared against a mid-interval storm.
     *
     * @param track the storm timeline, non-null
     * @param assets the assets to assess, non-null and non-empty; each element is assessed once, so
     *        callers should supply distinct assets
     * @param atTime the instant to evaluate, non-null and within the track's timeline
     * @return an immutable list with one exposure per asset, ordered by
     *         {@link AssetExposure#BY_SEVERITY}
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code assets} is empty or {@code atTime} lies outside the
     *         timeline
     */
    List<AssetExposure> calculate(CycloneTrack track, Collection<InfrastructureAsset> assets, Instant atTime);
}
