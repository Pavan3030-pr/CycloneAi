package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.adapter.out.feed.NoaaGeoJsonTrackParser;
import com.enterprise.cyclone.application.AdvisoryLanguage;
import com.enterprise.cyclone.application.CapAlertWriter;
import com.enterprise.cyclone.application.CycloneImpactService;
import com.enterprise.cyclone.application.ImpactReport;
import com.enterprise.cyclone.config.CapAlertPublication;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 *
 * <p>The CAP variant is the same use case in a second representation rather than a second code path:
 * one assessment runs, and the response is either JSON for this console or a CAP 1.2 alert for
 * every other warning system on the coast.
 */
@Validated
@RestController
@RequestMapping(path = "/api/v1/impact-assessments")
public class CycloneImpactController {

    /** Media type of a CAP 1.2 alert document. */
    private static final MediaType CAP_MEDIA_TYPE = MediaType.parseMediaType("application/cap+xml;charset=UTF-8");

    private final CycloneImpactService impactService;
    private final NoaaGeoJsonTrackParser trackParser;
    private final CapAlertWriter capAlertWriter;
    private final CapAlertPublication capAlertPublication;

    public CycloneImpactController(
            CycloneImpactService impactService,
            NoaaGeoJsonTrackParser trackParser,
            CapAlertWriter capAlertWriter,
            CapAlertPublication capAlertPublication) {
        this.impactService = impactService;
        this.trackParser = trackParser;
        this.capAlertWriter = capAlertWriter;
        this.capAlertPublication = capAlertPublication;
    }

    /**
     * Assesses a structured storm track against a set of assets.
     *
     * @param request the assessment request, validated before mapping
     * @return the impact assessment
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ImpactAssessmentResponse assess(@Valid @RequestBody ImpactAssessmentRequest request) {
        CycloneTrack track = ImpactAssessmentMapper.toTrack(request.track());
        List<InfrastructureAsset> assets = ImpactAssessmentMapper.toAssets(request.assets());
        return ImpactAssessmentMapper.toResponse(
                assess(track, assets, request.evaluationTime(), languageOf(request.language())));
    }

    /**
     * Assesses a track exactly as the upstream agency publishes it, without an intermediate
     * client-side reshaping of the GeoJSON.
     *
     * @param request the GeoJSON assessment request, validated before parsing
     * @return the impact assessment
     */
    @PostMapping(path = "/from-geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ImpactAssessmentResponse assessFromGeoJson(@Valid @RequestBody GeoJsonImpactAssessmentRequest request) {
        CycloneTrack track = trackParser.parse(request.featureCollection());
        List<InfrastructureAsset> assets = ImpactAssessmentMapper.toAssets(request.assets());
        return ImpactAssessmentMapper.toResponse(
                assess(track, assets, request.evaluationTime(), languageOf(request.language())));
    }

    /**
     * Assesses a structured track and returns the result as a CAP 1.2 alert.
     *
     * <p>Offered as a download so the same numbers that a control room reads on screen can be handed
     * to any system that consumes CAP — a state emergency operations centre, an aggregator, or the
     * cell-broadcast chain — without either side writing a bespoke integration.
     *
     * @param request the assessment request, validated before mapping
     * @return the alert document, as {@code application/cap+xml}
     */
    @PostMapping(path = "/cap", produces = "application/cap+xml")
    public ResponseEntity<String> assessAsCapAlert(@Valid @RequestBody ImpactAssessmentRequest request) {
        CycloneTrack track = ImpactAssessmentMapper.toTrack(request.track());
        List<InfrastructureAsset> assets = ImpactAssessmentMapper.toAssets(request.assets());
        AdvisoryLanguage language = languageOf(request.language());
        ImpactReport report = assess(track, assets, request.evaluationTime(), language);

        String document = capAlertWriter.write(report, language, capAlertPublication.options());
        String filename = capAlertWriter.identifier(report, language) + ".xml";

        return ResponseEntity.ok()
                .contentType(CAP_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(document);
    }

    private ImpactReport assess(
            CycloneTrack track, List<InfrastructureAsset> assets, Instant evaluationTime, AdvisoryLanguage language) {
        return evaluationTime == null
                ? impactService.assess(track, assets, track.endTime(), language)
                : impactService.assess(track, assets, evaluationTime, language);
    }

    /** An absent or unrecognised language code falls back to English rather than failing the request. */
    private static AdvisoryLanguage languageOf(String code) {
        return AdvisoryLanguage.fromCode(code);
    }
}
