package com.enterprise.cyclone.application;

/**
 * Raised when a requested asset is not in the registry.
 *
 * <p>Distinct from {@link IllegalArgumentException} because the caller's request was well formed and
 * the answer is simply "not found": the web layer maps this to 404 and the other to 400.
 */
public class AssetNotFoundException extends RuntimeException {

    private final transient String assetId;

    public AssetNotFoundException(String assetId) {
        super("Asset '" + assetId + "' is not registered");
        this.assetId = assetId;
    }

    /**
     * The identifier that was not found.
     */
    public String assetId() {
        return assetId;
    }
}
