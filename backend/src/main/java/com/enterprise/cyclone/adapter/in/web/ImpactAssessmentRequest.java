package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Request to assess a storm track against a set of assets.
 *
 * <p>Every collection is size-capped. A track of two hundred fixes covers a full five-day forecast
 * at six-hourly resolution with room to spare, and the caps matter because the cost of an assessment
 * is linear in the number of fixes times the number of assets: without a ceiling, one request could
 * occupy a request thread for an arbitrary length of time.
 *
 * @param track the storm timeline to assess, non-null; fixes may arrive in any order
 * @param assets the assets to assess, non-null, non-empty and at most 500 entries
 * @param evaluationTime the instant to evaluate, or null to evaluate at the track's latest fix
 */
public record ImpactAssessmentRequest(
        @NotNull @Valid TrackRequest track,
        @NotNull @NotEmpty @Size(max = 500) @Valid List<AssetRequest> assets,
        Instant evaluationTime) {

    /**
     * The storm timeline in wire form.
     *
     * @param stormId the agency storm identifier, for example {@code WP0726}; letters, digits, dot,
     *        underscore and hyphen only
     * @param points the fixes, non-null, non-empty and at most 200 entries
     */
    public record TrackRequest(
            @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9._-]+") String stormId,
            @NotNull @NotEmpty @Size(max = 200) @Valid List<TrackPointRequest> points) {
    }

    /**
     * One fix in wire form. Bounds mirror {@link CycloneTrackPoint} and {@link GeoCoordinate} so a
     * bad value is rejected with a field-level message before the domain is constructed.
     *
     * @param timestamp the valid time of the fix, RFC 3339, non-null
     */
    public record TrackPointRequest(
            @NotNull
            @Min((long) GeoCoordinate.MIN_LATITUDE)
            @Max((long) GeoCoordinate.MAX_LATITUDE)
            Double latitude,
            @NotNull
            @Min((long) GeoCoordinate.MIN_LONGITUDE)
            @Max((long) GeoCoordinate.MAX_LONGITUDE)
            Double longitude,
            @NotNull
            @Min(CycloneTrackPoint.MIN_WIND_SPEED_KNOTS)
            @Max(CycloneTrackPoint.MAX_WIND_SPEED_KNOTS)
            Integer windSpeedKnots,
            @NotNull
            @Min(CycloneTrackPoint.MIN_CENTRAL_PRESSURE_MB)
            @Max(CycloneTrackPoint.MAX_CENTRAL_PRESSURE_MB)
            Integer centralPressureMb,
            @NotNull Instant timestamp) {
    }
}
