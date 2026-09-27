package com.enterprise.cyclone.application;

import com.enterprise.cyclone.application.port.AssetRegistry;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import java.util.List;
import java.util.Objects;

/**
 * Use cases over the tracked asset inventory.
 *
 * <p>Thin by design: the registry adapter owns storage, the domain owns validation, and this class
 * owns only the decisions that belong to a use case, such as what to do when an asset is missing.
 */
public final class AssetRegistryService {

    private final AssetRegistry assetRegistry;

    public AssetRegistryService(AssetRegistry assetRegistry) {
        this.assetRegistry = Objects.requireNonNull(assetRegistry, "assetRegistry must not be null");
    }

    /**
     * Registers or replaces an asset.
     */
    public InfrastructureAsset register(InfrastructureAsset asset) {
        Objects.requireNonNull(asset, "asset must not be null");
        return assetRegistry.save(asset);
    }

    /**
     * Every registered asset, ordered by identifier.
     */
    public List<InfrastructureAsset> list() {
        return assetRegistry.findAll();
    }

    /**
     * One registered asset.
     *
     * @throws AssetNotFoundException when the identifier is not registered
     */
    public InfrastructureAsset requireById(String id) {
        return assetRegistry.findById(requireId(id))
                .orElseThrow(() -> new AssetNotFoundException(id));
    }

    /**
     * Removes an asset.
     *
     * @throws AssetNotFoundException when the identifier is not registered
     */
    public void remove(String id) {
        if (!assetRegistry.deleteById(requireId(id))) {
            throw new AssetNotFoundException(id);
        }
    }

    /**
     * Number of registered assets.
     */
    public long count() {
        return assetRegistry.count();
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("asset id must not be blank");
        }
        return id.strip();
    }
}
