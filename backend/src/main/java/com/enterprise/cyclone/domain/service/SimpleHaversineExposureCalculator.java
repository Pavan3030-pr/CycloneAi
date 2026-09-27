package com.enterprise.cyclone.domain.service;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.model.RiskLevel;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * First-order exposure model: great-circle distance from the interpolated storm centre combined
 * with an exponentially decaying wind field.
 *
 * <p>The wind at an asset is modelled as
 * {@code v(d) = vMax * exp(-d / 75 nm)}, where {@code vMax} is the interpolated maximum sustained
 * wind of the storm. The 75 nm e-folding distance reproduces the observed scale of a mature
 * system's 34 kt gale radius, so the modelled field assigns gale force to roughly the published
 * gale radius of a category 1-2 system and degrades to calm beyond ~250 nm.
 *
 * <p>The model deliberately ignores quadrant asymmetry, translation-induced asymmetry, terrain
 * interaction, gust factor and the forecast cone's positional uncertainty. It is a screening model:
 * it answers "which assets deserve attention first", not "what will the wind be at this address".
 * Those refinements belong in a replacement {@link ExposureCalculator} fed by observational data.
 */
public final class SimpleHaversineExposureCalculator implements ExposureCalculator {

    /** e-folding distance of the modelled wind field, in nautical miles. */
    private static final double WIND_FIELD_DECAY_NM = 75.0;

    /** NHC/JTWC gale-force (34 kt) radius of a mature system. */
    private static final double GALE_FORCE_BAND_NM = 120.0;
    /** 50 kt damaging-wind (R50) radius, the threshold published for exposed structures. */
    private static final double DAMAGING_WIND_BAND_NM = 60.0;
    /** Distance inside which hurricane-force winds are assumed regardless of modelled decay. */
    private static final double HURRICANE_FORCE_BAND_NM = 30.0;

    private static final int GALE_FORCE_KNOTS = SaffirSimpsonCategory.TS.minWindSpeedKnots();
    private static final int DAMAGING_WIND_KNOTS = 50;
    private static final int HURRICANE_FORCE_KNOTS = SaffirSimpsonCategory.CAT1.minWindSpeedKnots();

    @Override
    public List<AssetExposure> calculate(CycloneTrack track, Collection<InfrastructureAsset> assets, Instant atTime) {
        Objects.requireNonNull(track, "track must not be null");
        Objects.requireNonNull(assets, "assets must not be null");
        Objects.requireNonNull(atTime, "atTime must not be null");
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("assets must contain at least one asset");
        }

        CycloneTrackPoint storm = track.interpolateAt(atTime);
        List<AssetExposure> exposures = new ArrayList<>(assets.size());
        for (InfrastructureAsset asset : assets) {
            exposures.add(assess(asset, storm));
        }
        exposures.sort(AssetExposure.BY_SEVERITY);
        return List.copyOf(exposures);
    }

    private static AssetExposure assess(InfrastructureAsset asset, CycloneTrackPoint storm) {
        double distance = storm.coordinate().distanceNauticalMilesTo(asset.coordinate());
        int wind = estimateWindAtAsset(storm.windSpeedKnots(), distance);
        RiskLevel level = riskLevelFor(distance, wind);
        return new AssetExposure(asset, distance, wind, level, rationale(level, distance, wind));
    }

    /**
     * Wind at a point {@code distance} nautical miles from the centre, in knots.
     */
    private static int estimateWindAtAsset(int maximumWindKnots, double distance) {
        double decayed = maximumWindKnots * Math.exp(-distance / WIND_FIELD_DECAY_NM);
        return (int) Math.round(decayed);
    }

    /**
     * Applies the screening rules: a level is met when either the modelled wind reaches its
     * threshold or the asset sits inside the distance band for that threshold. The two criteria are
     * independent on purpose, so a strong-but-small storm and a weak-but-large storm are both
     * classified by whichever field reaches further.
     */
    private static RiskLevel riskLevelFor(double distance, int estimatedWind) {
        if (estimatedWind >= HURRICANE_FORCE_KNOTS || distance <= HURRICANE_FORCE_BAND_NM) {
            return RiskLevel.CRITICAL;
        }
        if (estimatedWind >= DAMAGING_WIND_KNOTS || distance <= DAMAGING_WIND_BAND_NM) {
            return RiskLevel.HIGH;
        }
        if (estimatedWind >= GALE_FORCE_KNOTS || distance <= GALE_FORCE_BAND_NM) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private static String rationale(RiskLevel level, double distance, int estimatedWind) {
        return switch (level) {
            case CRITICAL -> estimatedWind >= HURRICANE_FORCE_KNOTS
                    ? format("modelled wind of %d kt at the asset reaches hurricane force (%d kt)",
                            estimatedWind, HURRICANE_FORCE_KNOTS)
                    : format("asset lies %.0f nm from the storm centre, inside the %.0f nm hurricane-force band",
                            distance, HURRICANE_FORCE_BAND_NM);
            case HIGH -> estimatedWind >= DAMAGING_WIND_KNOTS
                    ? format("modelled wind of %d kt at the asset reaches the %d kt damaging-wind threshold",
                            estimatedWind, DAMAGING_WIND_KNOTS)
                    : format("asset lies %.0f nm from the storm centre, inside the %.0f nm damaging-wind band",
                            distance, DAMAGING_WIND_BAND_NM);
            case MEDIUM -> estimatedWind >= GALE_FORCE_KNOTS
                    ? format("modelled wind of %d kt at the asset reaches gale force (%d kt)",
                            estimatedWind, GALE_FORCE_KNOTS)
                    : format("asset lies %.0f nm from the storm centre, inside the %.0f nm gale-force band",
                            distance, GALE_FORCE_BAND_NM);
            case LOW -> format(
                    "modelled wind of %d kt at the asset and a distance of %.0f nm stay below every gale-force threshold",
                    estimatedWind, distance);
        };
    }

    private static String format(String template, Object... arguments) {
        return String.format(Locale.ROOT, template, arguments);
    }
}
