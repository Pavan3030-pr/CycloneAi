package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.adapter.in.web.ImpactAssessmentRequest.TrackPointRequest;
import com.enterprise.cyclone.adapter.in.web.ImpactAssessmentRequest.TrackRequest;
import com.enterprise.cyclone.application.ImpactReport;
import com.enterprise.cyclone.application.ImpactSummary;
import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.util.List;

/**
 * Translates between the transport representation and the domain model.
 *
 * <p>Conversion is one-way and total: every request field maps to exactly one domain concept, and
 * no domain object is ever exposed to Jackson. Boxed request numbers are unboxed here, which is
 * safe only because bean validation has already rejected nulls before this code runs.
 */
public final class ImpactAssessmentMapper {

    private ImpactAssessmentMapper() {
    }

    /**
     * Builds the storm aggregate from its wire form; ordering and duplicate rejection happen inside
     * {@link CycloneTrack}.
     */
    public static CycloneTrack toTrack(TrackRequest request) {
        List<CycloneTrackPoint> points = request.points().stream()
                .map(ImpactAssessmentMapper::toTrackPoint)
                .toList();
        return new CycloneTrack(request.stormId(), points);
    }

    /**
     * Builds the asset collection from its wire form.
     */
    public static List<InfrastructureAsset> toAssets(List<AssetRequest> requests) {
        return requests.stream().map(ImpactAssessmentMapper::toAsset).toList();
    }

    /**
     * Builds one asset from its wire form.
     */
    public static InfrastructureAsset toAsset(AssetRequest request) {
        return new InfrastructureAsset(
                request.id(),
                request.name(),
                request.assetType(),
                new GeoCoordinate(request.latitude(), request.longitude()));
    }

    /**
     * Renders an asset for transport.
     */
    public static AssetResponse toAssetResponse(InfrastructureAsset asset) {
        return new AssetResponse(
                asset.id(),
                asset.name(),
                asset.assetType(),
                asset.coordinate().latitude(),
                asset.coordinate().longitude(),
                asset.coordinateString());
    }

    /**
     * Renders a report for transport, deriving the storm's category from its interpolated wind.
     */
    public static ImpactAssessmentResponse toResponse(ImpactReport report) {
        CycloneTrackPoint storm = report.stormPosition();
        return new ImpactAssessmentResponse(
                report.stormId(),
                report.evaluatedAt(),
                new ImpactAssessmentResponse.StormPositionResponse(
                        storm.latitude(),
                        storm.longitude(),
                        storm.windSpeedKnots(),
                        storm.centralPressureMb(),
                        SaffirSimpsonCategory.fromWindSpeedKnots(storm.windSpeedKnots()),
                        storm.coordinateString()),
                toSummaryResponse(report.summary()),
                report.advisory(),
                report.exposures().stream().map(ImpactAssessmentMapper::toExposureResponse).toList());
    }

    private static CycloneTrackPoint toTrackPoint(TrackPointRequest request) {
        return new CycloneTrackPoint(
                request.latitude(),
                request.longitude(),
                request.windSpeedKnots(),
                request.centralPressureMb(),
                request.timestamp());
    }

    private static ImpactAssessmentResponse.ImpactSummaryResponse toSummaryResponse(ImpactSummary summary) {
        return new ImpactAssessmentResponse.ImpactSummaryResponse(
                summary.totalAssets(),
                summary.countByRisk(),
                summary.highestRisk(),
                summary.nearestDistanceNauticalMiles(),
                summary.peakEstimatedWindKnots());
    }

    private static ImpactAssessmentResponse.AssetExposureResponse toExposureResponse(AssetExposure exposure) {
        return new ImpactAssessmentResponse.AssetExposureResponse(
                exposure.asset().id(),
                exposure.asset().name(),
                exposure.asset().assetType(),
                exposure.asset().coordinateString(),
                exposure.distanceNauticalMiles(),
                exposure.estimatedWindAtAsset(),
                exposure.riskLevel(),
                exposure.rationale());
    }
}
