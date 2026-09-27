package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.adapter.out.feed.NoaaGeoJsonTrackParser;
import com.enterprise.cyclone.application.CycloneImpactService;
import com.enterprise.cyclone.application.ImpactReport;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound HTTP adapter for the impact assessment use case.
 *
 * <p>The controller does three things and nothing else: deserialise, delegate, map. Ordering,
 * interpolation, risk scoring and advisory wording all happen inside the core, so the same use case
 * is reachable from any other inbound adapter without duplication.
 *
 * <p>Post is used for a read-shaped operation because the storm timeline alone can run to hundreds of
 * fixes; a query string is the wrong container for it.
 */
@Validated
@RestController
@RequestMapping(path = "/api/v1/impact-assessments", produces = MediaType.APPLICATION_JSON_VALUE)
public class CycloneImpactController {

    private final CycloneImpactService impactService;
    private final NoaaGeoJsonTrackParser trackParser;

    public CycloneImpactController(CycloneImpactService impactService, NoaaGeoJsonTrackParser trackParser) {
        this.impactService = impactService;
        this.trackParser = trackParser;
    }

    /**
     * Assesses a structured storm track against a set of assets.
     *
     * @param request the assessment request, validated before mapping
     * @return the impact assessment
     */
    @PostMapping
    public ImpactAssessmentResponse assess(@Valid @RequestBody ImpactAssessmentRequest request) {
        CycloneTrack track = ImpactAssessmentMapper.toTrack(request.track());
        List<InfrastructureAsset> assets = ImpactAssessmentMapper.toAssets(request.assets());
        return ImpactAssessmentMapper.toResponse(assess(track, assets, request.evaluationTime()));
    }

    /**
     * Assesses a track exactly as the upstream agency publishes it, without an intermediate
     * client-side reshaping of the GeoJSON.
     *
     * @param request the GeoJSON assessment request, validated before parsing
     * @return the impact assessment
     */
    @PostMapping(path = "/from-geojson")
    public ImpactAssessmentResponse assessFromGeoJson(@Valid @RequestBody GeoJsonImpactAssessmentRequest request) {
        CycloneTrack track = trackParser.parse(request.featureCollection());
        List<InfrastructureAsset> assets = ImpactAssessmentMapper.toAssets(request.assets());
        return ImpactAssessmentMapper.toResponse(assess(track, assets, request.evaluationTime()));
    }

    private ImpactReport assess(CycloneTrack track, List<InfrastructureAsset> assets, Instant evaluationTime) {
        return evaluationTime == null
                ? impactService.assess(track, assets)
                : impactService.assess(track, assets, evaluationTime);
    }
}
