package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One infrastructure asset submitted for assessment.
 *
 * <p>Coordinates are boxed and annotated so that an omitted latitude is rejected as a missing field
 * instead of defaulting to {@code 0.0}, which would place the asset in the Gulf of Guinea. The
 * bounds are taken from {@link GeoCoordinate} so the wire contract and the domain cannot drift, and
 * the text fields are length-capped so an oversized payload fails validation rather than reaching
 * the registry.
 */
public record AssetRequest(
        @NotBlank @Size(max = 64) String id,
        @NotBlank @Size(max = 256) String name,
        @NotNull AssetType assetType,
        @NotNull
        @DecimalMin("" + GeoCoordinate.MIN_LATITUDE)
        @DecimalMax("" + GeoCoordinate.MAX_LATITUDE)
        Double latitude,
        @NotNull
        @DecimalMin("" + GeoCoordinate.MIN_LONGITUDE)
        @DecimalMax("" + GeoCoordinate.MAX_LONGITUDE)
        Double longitude) {
}
