package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.RiskLevel;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Assessment result returned to a client.
 *
 * <p>Every value is derived from immutable domain objects at mapping time, so the response is a
 * snapshot that stays consistent with the advisory text it carries.
 *
 * @param stormId the assessed storm
 * @param evaluatedAt the instant the storm was evaluated at
 * @param stormPosition the interpolated storm state, not necessarily a published fix
 * @param summary aggregate statistics over the exposures
 * @param advisory the rendered early-warning text
 * @param advisoryProvenance which generator wrote the advisory, in which language, and how long it
 *        took, so a client can label model-written text differently from template text
 * @param exposures one entry per assessed asset, most exposed first
 */
public record ImpactAssessmentResponse(
        String stormId,
        Instant evaluatedAt,
        StormPositionResponse stormPosition,
        ImpactSummaryResponse summary,
        String advisory,
        AdvisoryProvenanceResponse advisoryProvenance,
        List<AssetExposureResponse> exposures) {

    /**
     * How the advisory was produced.
     *
     * @param generator {@code gemini} or {@code deterministic}
     * @param model the model identifier, omitted when the deterministic generator wrote the text
     * @param language the language the advisory was issued in
     * @param degraded true when a model was attempted and failed, so the fallback ran
     * @param detail why the fallback ran, or why no model was used
     */
    public record AdvisoryProvenanceResponse(
            String generator,
            String model,
            String language,
            long latencyMillis,
            boolean degraded,
            String detail) {
    }

    /**
     * The interpolated storm state.
     *
     * @param category the Saffir-Simpson category of {@code windSpeedKnots}
     * @param coordinateString the position in GeoJSON order, {@code "longitude,latitude"}
     */
    public record StormPositionResponse(
            double latitude,
            double longitude,
            int windSpeedKnots,
            int centralPressureMb,
            SaffirSimpsonCategory category,
            String coordinateString) {
    }

    /**
     * Aggregate statistics.
     *
     * @param countByRisk asset count per risk level, in ascending severity order
     * @param nearestDistanceNauticalMiles distance to the closest assessed asset
     * @param peakEstimatedWindKnots highest modelled wind across all assessed assets
     */
    public record ImpactSummaryResponse(
            int totalAssets,
            Map<RiskLevel, Integer> countByRisk,
            RiskLevel highestRisk,
            double nearestDistanceNauticalMiles,
            int peakEstimatedWindKnots) {
    }

    /**
     * One asset's exposure.
     *
     * @param rationale why this asset received this level
     */
    public record AssetExposureResponse(
            String assetId,
            String assetName,
            AssetType assetType,
            String coordinateString,
            double distanceNauticalMiles,
            int estimatedWindAtAsset,
            RiskLevel riskLevel,
            String rationale) {
    }
}
