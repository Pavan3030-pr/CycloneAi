package com.enterprise.cyclone.adapter.out.feed;

import com.enterprise.cyclone.domain.model.CycloneTrack;
import java.util.List;

/**
 * Contract for sourcing cyclone timelines from an upstream agency instead of a request body.
 *
 * <p>This is an extension seam, not a stub: {@link NoaaGeoJsonTrackParser} already turns a published
 * feature collection into a {@link CycloneTrack}, so an implementation of this interface is an HTTP
 * client plus that parser. The interface lives outside the core because no use case consumes it yet;
 * when one does, it moves up into {@code application.port} so the dependency arrow keeps pointing
 * inwards.
 *
 * <p>Implementations must not return null and must be safe to call concurrently.
 */
public interface CycloneTrackFeed {

    /**
     * Identifiers of the storms the upstream agency is currently tracking, in its own identifier
     * format (for example {@code WP0726}).
     *
     * @return a non-null, possibly empty list of storm identifiers
     */
    List<String> activeStormIds();

    /**
     * The latest published timeline for one storm: observed fixes followed by forecast positions.
     *
     * @param stormId the upstream storm identifier, non-null and non-blank
     * @return the storm timeline, never null
     * @throws IllegalArgumentException if the identifier is blank or unknown to the upstream agency
     * @throws IllegalStateException if the upstream product cannot be retrieved or decoded
     */
    CycloneTrack fetchTrack(String stormId);
}
