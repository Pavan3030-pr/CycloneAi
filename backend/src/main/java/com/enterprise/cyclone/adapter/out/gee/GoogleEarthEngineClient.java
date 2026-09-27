package com.enterprise.cyclone.adapter.out.gee;

import com.enterprise.cyclone.domain.model.GeoCoordinate;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * Contract for observational evidence from Google Earth Engine.
 *
 * <p>Exposure scoring from track geometry alone cannot see what already happened on the ground:
 * whether a road is inundated, whether a roof is gone, whether a shelter is still reachable. This
 * port is where that evidence enters, as observed fractions and imagery references rather than as
 * modelled values, so a scoring strategy can combine prediction with observation.
 *
 * <p>Both methods return {@link Optional#empty()} for a legitimate absence of observation, which is
 * routine &mdash; cloud cover, a revisit gap, a sensor outage. Absence is not an error and must not
 * be signalled as one, because a caller that cannot distinguish "no data" from "no damage" will
 * under-warn. Implementations must be thread-safe and must not return null.
 */
public interface GoogleEarthEngineClient {

    /**
     * Fraction of the area within {@code radiusNauticalMiles} of {@code centre} that the most recent
     * cloud-free scene shows as inundated.
     *
     * @param centre the area of interest, non-null
     * @param radiusNauticalMiles the sampling radius, greater than zero
     * @param observedAt the instant to observe at, non-null
     * @return the inundated fraction in the range 0.0 to 1.0, or empty when no usable scene exists
     * @throws IllegalArgumentException if {@code radiusNauticalMiles} is not positive
     * @throws IllegalStateException if Earth Engine cannot be reached or the computation fails
     */
    Optional<Double> inundationFraction(GeoCoordinate centre, double radiusNauticalMiles, Instant observedAt);

    /**
     * Reference to the imagery behind an observation, suitable for handing to a multimodal model
     * alongside an advisory.
     *
     * @param centre the area of interest, non-null
     * @param observedAt the instant to observe at, non-null
     * @return a time-bounded URI to the exported scene, or empty when none is available
     * @throws IllegalStateException if Earth Engine cannot be reached or the export fails
     */
    Optional<URI> imageryUri(GeoCoordinate centre, Instant observedAt);
}
