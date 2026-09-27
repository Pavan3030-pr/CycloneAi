package com.enterprise.cyclone.domain.model;

import java.util.Objects;

/**
 * A critical infrastructure asset exposed to cyclone hazard.
 *
 * <p>{@code id} is the stable natural key assigned by the source asset registry (for example an
 * HIFLD or OpenStreetMap identifier) and is the only field assumed unique and immutable across
 * publications of the same asset.
 */
public record InfrastructureAsset(
        String id,
        String name,
        AssetType assetType,
        GeoCoordinate coordinate) {

    public InfrastructureAsset {
        id = requireNonBlank(id, "id");
        name = requireNonBlank(name, "name");
        Objects.requireNonNull(assetType, "assetType must not be null");
        Objects.requireNonNull(coordinate, "coordinate must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    /**
     * The asset location rendered as a GeoJSON position string, i.e. {@code "longitude,latitude"}.
     */
    public String coordinateString() {
        return coordinate.asCoordinateString();
    }
}
