package com.enterprise.cyclone.adapter.out.registry;

import com.enterprise.cyclone.application.port.AssetRegistry;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry adapter backed by the heap.
 *
 * <p>The map is concurrent, so registration and assessment can run in parallel without locking, and
 * reads never observe a partially published asset because {@link InfrastructureAsset} is immutable.
 * Deliberately not distributed: for a single-instance demo this is the whole storage story, and
 * swapping in a database means implementing {@link AssetRegistry} again, not changing callers.
 */
public final class InMemoryAssetRegistry implements AssetRegistry {

    private final Map<String, InfrastructureAsset> assets = new ConcurrentHashMap<>();

    @Override
    public InfrastructureAsset save(InfrastructureAsset asset) {
        Objects.requireNonNull(asset, "asset must not be null");
        assets.put(asset.id(), asset);
        return asset;
    }

    @Override
    public Optional<InfrastructureAsset> findById(String id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(assets.get(id));
    }

    @Override
    public List<InfrastructureAsset> findAll() {
        return assets.values().stream()
                .sorted(Comparator.comparing(InfrastructureAsset::id))
                .toList();
    }

    @Override
    public boolean deleteById(String id) {
        Objects.requireNonNull(id, "id must not be null");
        return assets.remove(id) != null;
    }

    @Override
    public long count() {
        return assets.size();
    }
}
