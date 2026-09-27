package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.application.AssetRegistryService;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the tracked asset inventory.
 *
 * <p>Reads require any authenticated role; writes require {@code ANALYST} or {@code ADMIN}, enforced
 * in the security filter chain rather than here, so the rule holds for every current and future
 * caller of these paths.
 */
@Validated
@RestController
@RequestMapping(path = "/api/v1/assets", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetController {

    private final AssetRegistryService assetRegistryService;

    public AssetController(AssetRegistryService assetRegistryService) {
        this.assetRegistryService = assetRegistryService;
    }

    @Operation(summary = "List every registered asset, ordered by identifier")
    @GetMapping
    public List<AssetResponse> list() {
        return assetRegistryService.list().stream()
                .map(ImpactAssessmentMapper::toAssetResponse)
                .toList();
    }

    @Operation(summary = "Fetch one registered asset")
    @GetMapping("/{id}")
    public AssetResponse find(@PathVariable @NotBlank @Size(max = 64) String id) {
        return ImpactAssessmentMapper.toAssetResponse(assetRegistryService.requireById(id));
    }

    @Operation(summary = "Register an asset, replacing any asset with the same identifier")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse register(@Valid @RequestBody AssetRequest request) {
        InfrastructureAsset asset = assetRegistryService.register(ImpactAssessmentMapper.toAsset(request));
        return ImpactAssessmentMapper.toAssetResponse(asset);
    }

    @Operation(summary = "Remove an asset from the registry")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable @NotBlank @Size(max = 64) String id) {
        assetRegistryService.remove(id);
    }
}
