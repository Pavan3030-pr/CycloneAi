package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.domain.model.AssetType;

/**
 * A registered infrastructure asset as returned to clients.
 *
 * @param coordinateString the position in GeoJSON order, {@code "longitude,latitude"}
 */
public record AssetResponse(
        String id,
        String name,
        AssetType assetType,
        double latitude,
        double longitude,
        String coordinateString) {
}
