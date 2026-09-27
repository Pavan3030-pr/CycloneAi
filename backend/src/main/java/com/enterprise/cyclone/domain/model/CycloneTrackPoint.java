package com.enterprise.cyclone.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single position in a tropical cyclone track: either an observed fix or a forecast point.
 *
 * <p>Field semantics mirror the attribute set of NOAA/NHC and JTWC best-track and forecast
 * GeoJSON feature properties (and their ATCF b-deck/A-deck source columns):
 *
 * <ul>
 *   <li>{@code latitude} / {@code longitude} &ndash; WGS84 storm centre fix, ATCF {@code LAT}/{@code LON}</li>
 *   <li>{@code windSpeedKnots} &ndash; 1-minute maximum sustained surface wind, ATCF {@code VMAX}</li>
 *   <li>{@code centralPressureMb} &ndash; minimum sea-level pressure, ATCF {@code MSLP}</li>
 *   <li>{@code timestamp} &ndash; valid time of the fix, UTC</li>
 * </ul>
 *
 * <p>Coordinates are held as primitives rather than a {@link GeoCoordinate} because a track is an
 * append-only sequence of thousands of points and because lat/lon arrive as separate numeric
 * properties in the wire format. Use {@link #coordinate()} when a value object is needed.
 *
 * <p>Wind and pressure are integral because it is how both agencies publish them, and the bounds
 * act as feed sanity guards against malformed upstream data.
 */
public record CycloneTrackPoint(
        double latitude,
        double longitude,
        int windSpeedKnots,
        int centralPressureMb,
        Instant timestamp) {

    /** Weakest reportable system; below this it is not classified as a tropical cyclone. */
    public static final int MIN_WIND_SPEED_KNOTS = 0;
    /** Above the strongest 1-minute sustained wind ever recorded (Typhoon Haiyan, ~195 kt). */
    public static final int MAX_WIND_SPEED_KNOTS = 250;
    /** Below the deepest sea-level pressure ever recorded (Typhoon Tip, 870 mb). */
    public static final int MIN_CENTRAL_PRESSURE_MB = 850;
    /** Above this a system would be a deep mid-latitude low, not a tropical cyclone centre. */
    public static final int MAX_CENTRAL_PRESSURE_MB = 1025;

    public CycloneTrackPoint {
        GeoCoordinate.validate(latitude, longitude);
        if (windSpeedKnots < MIN_WIND_SPEED_KNOTS || windSpeedKnots > MAX_WIND_SPEED_KNOTS) {
            throw new IllegalArgumentException(
                    "windSpeedKnots must be within [" + MIN_WIND_SPEED_KNOTS + ", " + MAX_WIND_SPEED_KNOTS
                            + "] knots but was " + windSpeedKnots);
        }
        if (centralPressureMb < MIN_CENTRAL_PRESSURE_MB || centralPressureMb > MAX_CENTRAL_PRESSURE_MB) {
            throw new IllegalArgumentException(
                    "centralPressureMb must be within [" + MIN_CENTRAL_PRESSURE_MB + ", "
                            + MAX_CENTRAL_PRESSURE_MB + "] mb but was " + centralPressureMb);
        }
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    /**
     * The storm centre fix as a geospatial value object.
     */
    public GeoCoordinate coordinate() {
        return new GeoCoordinate(latitude, longitude);
    }

    /**
     * The storm centre rendered as a GeoJSON position string, i.e. {@code "longitude,latitude"}.
     *
     * <p>Note the RFC 7946 order: longitude first.
     */
    public String coordinateString() {
        return GeoCoordinate.coordinateString(longitude, latitude);
    }
}
