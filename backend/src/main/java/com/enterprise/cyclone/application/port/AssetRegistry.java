package com.enterprise.cyclone.application.port;

import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import java.util.List;
import java.util.Optional;

/**
 * Port for the durable set of tracked infrastructure assets.
 *
 * <p>Assets outlive any single storm: they are the inventory an agency curates once and reuses for
 * every assessment, which is why they are stored rather than passed in on each request. The port
 * says nothing about how they are stored, so an in-memory map for a demo and a spatial database
 * behind PostGIS are equally valid adapters.
 *
 * <p>Implementations must be thread-safe and must never return null.
 */
public interface AssetRegistry {

    /**
     * Stores an asset, replacing any existing entry with the same {@code id()}.
     *
     * @param asset the asset to store, non-null and already valid by construction
     * @return the stored asset
     */
    InfrastructureAsset save(InfrastructureAsset asset);

    /**
     * Looks up one asset by its registry identifier.
     *
     * @param id the registry identifier, non-null
     * @return the asset, or empty when this registry does not hold it
     */
    Optional<InfrastructureAsset> findById(String id);

    /**
     * Every stored asset, ordered by identifier so that responses are stable between calls.
     */
    List<InfrastructureAsset> findAll();

    /**
     * Removes an asset.
     *
     * @return true when an asset was removed, false when the identifier was not present
     */
    boolean deleteById(String id);

    /**
     * Number of stored assets.
     */
    long count();
}
