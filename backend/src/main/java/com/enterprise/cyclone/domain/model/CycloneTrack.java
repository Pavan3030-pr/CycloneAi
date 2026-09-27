package com.enterprise.cyclone.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root for one tropical cyclone: the storm identity and its complete, canonically
 * ordered timeline of fixes.
 *
 * <p>The aggregate owns a single invariant that every downstream component may rely on without
 * re-checking it: {@link #points()} is immutable, non-empty, and in strict ascending order of
 * {@link CycloneTrackPoint#timestamp()}, with no two points sharing a timestamp. That ordering is
 * established once here so that forecasting, interpolation and spatial joins are plain reads.
 *
 * <p>Points are stored in an array-backed immutable list, so indexed access is O(1) and iteration
 * is contiguous in memory &mdash; the access pattern of a spatial scan along a track. Both
 * {@link #pointAt(Instant)} and {@link #interpolateAt(Instant)} exploit the ordering for O(log n)
 * temporal lookup.
 *
 * <p>Being a record, the class is implicitly final, its fields are final, and equality is
 * structural: two tracks are the same aggregate exactly when they share an id and a timeline.
 */
public record CycloneTrack(String stormId, List<CycloneTrackPoint> points) {

    private static final double FULL_TURN_DEGREES = 360.0;
    private static final double HALF_TURN_DEGREES = 180.0;

    public CycloneTrack {
        stormId = requireNonBlank(stormId, "stormId");
        points = canonicalize(points, stormId);
    }

    /**
     * The earliest fix in the timeline.
     */
    public Instant startTime() {
        return points.get(0).timestamp();
    }

    /**
     * The latest fix in the timeline.
     */
    public Instant endTime() {
        return points.get(points.size() - 1).timestamp();
    }

    /**
     * Looks up the fix valid at exactly {@code time}.
     *
     * <p>Because timestamps are strictly ascending, this is a binary search: O(log n) comparisons
     * and no allocation. Returns {@link Optional#empty()} when the timeline has no fix at that
     * instant, which is the normal case between two synoptic times; use
     * {@link #interpolateAt(Instant)} when a value between fixes is required.
     *
     * @throws NullPointerException if {@code time} is null
     */
    public Optional<CycloneTrackPoint> pointAt(Instant time) {
        Objects.requireNonNull(time, "time must not be null");

        int index = lowerBound(time);
        return index < points.size() && points.get(index).timestamp().equals(time)
                ? Optional.of(points.get(index))
                : Optional.empty();
    }

    /**
     * Estimates the storm state at {@code time}, interpolating linearly between the surrounding
     * fixes.
     *
     * <p>Position is interpolated in latitude and longitude. The longitude delta is unwrapped
     * across the antimeridian first, so a track crossing 180&deg; moves by the short arc instead of
     * sweeping almost the whole globe. Wind and pressure are interpolated over the same fraction
     * and rounded to the nearest knot and millibar, matching the integral precision of the ATCF
     * products. The result is a fully validated {@link CycloneTrackPoint} stamped with {@code time}.
     *
     * @param time the instant to evaluate, inclusive of {@link #startTime()} and {@link #endTime()}
     * @return the exact fix if one exists at {@code time}, otherwise the interpolated state
     * @throws NullPointerException if {@code time} is null
     * @throws IllegalArgumentException if {@code time} lies outside the timeline
     */
    public CycloneTrackPoint interpolateAt(Instant time) {
        Objects.requireNonNull(time, "time must not be null");
        if (time.isBefore(startTime()) || time.isAfter(endTime())) {
            throw new IllegalArgumentException("time " + time + " is outside the timeline of storm '" + stormId
                    + "' which spans [" + startTime() + ", " + endTime() + "]");
        }

        int index = lowerBound(time);
        CycloneTrackPoint following = points.get(index);
        if (following.timestamp().equals(time)) {
            return following;
        }

        CycloneTrackPoint preceding = points.get(index - 1);
        double fraction = fractionBetween(preceding.timestamp(), time, following.timestamp());
        return new CycloneTrackPoint(
                preceding.latitude() + fraction * (following.latitude() - preceding.latitude()),
                interpolateLongitude(preceding.longitude(), following.longitude(), fraction),
                (int) Math.round(preceding.windSpeedKnots()
                        + fraction * (following.windSpeedKnots() - preceding.windSpeedKnots())),
                (int) Math.round(preceding.centralPressureMb()
                        + fraction * (following.centralPressureMb() - preceding.centralPressureMb())),
                time);
    }

    /**
     * Index of the first fix at or after {@code time}, or {@code points.size()} when {@code time}
     * is later than every fix.
     */
    private int lowerBound(Instant time) {
        int low = 0;
        int high = points.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (points.get(mid).timestamp().isBefore(time)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private static double fractionBetween(Instant from, Instant at, Instant to) {
        return secondsBetween(from, at) / secondsBetween(from, to);
    }

    /**
     * Duration in seconds with sub-second precision, taken from {@link Duration#getSeconds()} rather
     * than {@link Duration#toNanos()} so that a pathologically long timeline cannot overflow a long.
     */
    private static double secondsBetween(Instant from, Instant to) {
        Duration duration = Duration.between(from, to);
        return duration.getSeconds() + duration.getNano() / 1_000_000_000.0;
    }

    /**
     * Interpolates longitude along the short arc, wrapping the result back into the valid range.
     */
    private static double interpolateLongitude(double from, double to, double fraction) {
        double delta = to - from;
        if (delta > HALF_TURN_DEGREES) {
            delta -= FULL_TURN_DEGREES;
        } else if (delta < -HALF_TURN_DEGREES) {
            delta += FULL_TURN_DEGREES;
        }

        double longitude = from + fraction * delta;
        if (longitude > GeoCoordinate.MAX_LONGITUDE) {
            longitude -= FULL_TURN_DEGREES;
        } else if (longitude < GeoCoordinate.MIN_LONGITUDE) {
            longitude += FULL_TURN_DEGREES;
        }
        return longitude;
    }

    /**
     * Copies the supplied points into a caller-owned list, sorts them chronologically, rejects
     * repeated timestamps, and freezes the result.
     */
    private static List<CycloneTrackPoint> canonicalize(List<CycloneTrackPoint> points, String stormId) {
        Objects.requireNonNull(points, "points must not be null");
        if (points.isEmpty()) {
            throw new IllegalArgumentException("storm '" + stormId + "' must have at least one track point");
        }

        List<CycloneTrackPoint> ordered = new ArrayList<>(points.size());
        int index = 0;
        for (CycloneTrackPoint point : points) {
            if (point == null) {
                throw new NullPointerException("track point at index " + index + " must not be null");
            }
            ordered.add(point);
            index++;
        }

        ordered.sort(Comparator.comparing(CycloneTrackPoint::timestamp));
        assertDistinctTimestamps(ordered, stormId);
        return List.copyOf(ordered);
    }

    /**
     * Verifies the sorted timeline holds at most one fix per instant.
     *
     * <p>Duplicates are rejected rather than resolved because they are ambiguous: two fixes at the
     * same valid time can disagree on position or intensity, and silently keeping either one would
     * fabricate a storm history. The list is already sorted, so a single linear pass suffices.
     */
    private static void assertDistinctTimestamps(List<CycloneTrackPoint> ordered, String stormId) {
        Instant previous = null;
        for (CycloneTrackPoint point : ordered) {
            Instant current = point.timestamp();
            if (current.equals(previous)) {
                throw new IllegalArgumentException(
                        "storm '" + stormId + "' has multiple track points at timestamp " + current);
            }
            previous = current;
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
