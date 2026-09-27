package com.enterprise.cyclone.domain.model;

/**
 * Cyclone intensity on the scale NOAA/NHC publishes for the Atlantic and eastern Pacific, extended
 * downwards to the pre-hurricane stages so that a whole track maps to exactly one constant.
 *
 * <p>Boundaries are the official 1-minute maximum sustained wind thresholds in knots: 34 kt marks
 * tropical storm force, 64 kt hurricane force, and 96 kt the start of a major hurricane.
 */
public enum SaffirSimpsonCategory {

    TD(0, "Tropical Depression"),
    TS(34, "Tropical Storm"),
    CAT1(64, "Category 1 Hurricane"),
    CAT2(83, "Category 2 Hurricane"),
    CAT3(96, "Category 3 Hurricane"),
    CAT4(113, "Category 4 Hurricane"),
    CAT5(137, "Category 5 Hurricane");

    private final int minWindSpeedKnots;
    private final String displayName;

    SaffirSimpsonCategory(int minWindSpeedKnots, String displayName) {
        this.minWindSpeedKnots = minWindSpeedKnots;
        this.displayName = displayName;
    }

    /**
     * Lowest 1-minute sustained wind, in knots, that qualifies for this category.
     */
    public int minWindSpeedKnots() {
        return minWindSpeedKnots;
    }

    /**
     * Human-readable name, safe to place in advisories.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Classifies a 1-minute sustained wind speed.
     *
     * @throws IllegalArgumentException if {@code windSpeedKnots} is outside the range accepted by
     *         {@link CycloneTrackPoint}
     */
    public static SaffirSimpsonCategory fromWindSpeedKnots(int windSpeedKnots) {
        if (windSpeedKnots < 0 || windSpeedKnots > CycloneTrackPoint.MAX_WIND_SPEED_KNOTS) {
            throw new IllegalArgumentException("windSpeedKnots must be within [0, "
                    + CycloneTrackPoint.MAX_WIND_SPEED_KNOTS + "] knots but was " + windSpeedKnots);
        }

        SaffirSimpsonCategory matched = TD;
        for (SaffirSimpsonCategory category : values()) {
            if (windSpeedKnots < category.minWindSpeedKnots) {
                break;
            }
            matched = category;
        }
        return matched;
    }
}
