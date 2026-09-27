package com.enterprise.cyclone.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable geographic position on the WGS84 datum (EPSG:4326), the coordinate system used by
 * NOAA/NHC and JTWC GeoJSON products.
 *
 * <p>Serialised ordering follows <a href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.1.1">
 * RFC 7946 &sect;3.1.1</a>: a GeoJSON position array is {@code [longitude, latitude]}.
 */
public record GeoCoordinate(double latitude, double longitude) {

    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;

    /** Mean Earth radius, the sphere NHC/JTWC use for great-circle distance reporting. */
    private static final double EARTH_RADIUS_NAUTICAL_MILES = 3440.065;

    /**
     * Four decimal places is roughly 11 m at the equator, finer than the precision of any
     * operational cyclone fix.
     */
    private static final String POSITION_FORMAT = "%.4f,%.4f";

    public GeoCoordinate {
        validate(latitude, longitude);
    }

    /**
     * Validates a coordinate pair without allocating a value object.
     *
     * <p>The explicit finiteness test is required because {@code NaN} compares {@code false}
     * against every range check and would otherwise pass silently.
     *
     * @throws IllegalArgumentException if either value is non-finite or out of range
     */
    public static void validate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException(
                    "latitude must be a finite value within [" + MIN_LATITUDE + ", " + MAX_LATITUDE
                            + "] degrees but was " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException(
                    "longitude must be a finite value within [" + MIN_LONGITUDE + ", " + MAX_LONGITUDE
                            + "] degrees but was " + longitude);
        }
    }

    /**
     * Formats a pair in GeoJSON position order ({@code "longitude,latitude"}).
     *
     * <p>The formatter is pinned to {@link Locale#ROOT} so locales with a comma decimal separator
     * cannot corrupt machine-readable output.
     */
    public static String coordinateString(double longitude, double latitude) {
        return String.format(Locale.ROOT, POSITION_FORMAT, longitude, latitude);
    }

    /**
     * Renders this coordinate in GeoJSON position order ({@code "longitude,latitude"}).
     */
    public String asCoordinateString() {
        return coordinateString(longitude, latitude);
    }

    /**
     * Returns this coordinate as a GeoJSON position array, i.e. {@code [longitude, latitude]}.
     */
    public double[] asPositionArray() {
        return new double[] {longitude, latitude};
    }

    /**
     * Great-circle distance to another coordinate, in nautical miles.
     */
    public double distanceNauticalMilesTo(GeoCoordinate other) {
        Objects.requireNonNull(other, "other must not be null");

        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(other.longitude() - longitude);

        double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double centralAngle = 2 * Math.asin(Math.min(1.0, Math.sqrt(haversine)));

        return EARTH_RADIUS_NAUTICAL_MILES * centralAngle;
    }
}
