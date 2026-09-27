package com.enterprise.cyclone.adapter.in.web;

import com.enterprise.cyclone.adapter.out.feed.NoaaGeoJsonTrackParser;
import com.enterprise.cyclone.application.AdvisoryLanguage;
import com.enterprise.cyclone.application.AdvisoryProvenance;
import com.enterprise.cyclone.application.AdvisoryVocabulary;
import com.enterprise.cyclone.application.CapAlertWriter;
import com.enterprise.cyclone.application.CycloneImpactService;
import com.enterprise.cyclone.application.ImpactReport;
import com.enterprise.cyclone.application.port.AlertDispatcher;
import com.enterprise.cyclone.application.port.DispatchRequest;
import com.enterprise.cyclone.application.port.DispatchResult;
import com.enterprise.cyclone.config.CapAlertPublication;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes an advisory beyond this console.
 *
 * <p>The dispatch endpoint takes the same assessment request as any other assessment call and re-runs
 * the use case on the server. It deliberately does not accept pre-rendered text from the client: a
 * public endpoint that forwards caller-supplied prose to a messaging provider is a spam relay with
 * extra steps, and the numbers in a warning must come from the exposure model rather than from
 * whoever called the API.
 *
 * <p>Readiness is exposed separately so the console can tell an operator that no channel is
 * configured, instead of offering an action that quietly does nothing.
 */
@Validated
@RestController
@RequestMapping(path = "/api/v1/advisories")
public class AdvisoryDispatchController {

    private final CycloneImpactService impactService;
    private final NoaaGeoJsonTrackParser trackParser;
    private final CapAlertWriter capAlertWriter;
    private final CapAlertPublication capAlertPublication;
    private final AlertDispatcher alertDispatcher;

    public AdvisoryDispatchController(
            CycloneImpactService impactService,
            NoaaGeoJsonTrackParser trackParser,
            CapAlertWriter capAlertWriter,
            CapAlertPublication capAlertPublication,
            AlertDispatcher alertDispatcher) {
        this.impactService = impactService;
        this.trackParser = trackParser;
        this.capAlertWriter = capAlertWriter;
        this.capAlertPublication = capAlertPublication;
        this.alertDispatcher = alertDispatcher;
    }

    /**
     * Reports whether an outbound channel is wired up, so the interface can be honest about it.
     */
    @GetMapping(path = "/channel", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdvisoryChannelResponse channel() {
        boolean configured = alertDispatcher.configured();
        return new AdvisoryChannelResponse(
                alertDispatcher.channel(),
                configured,
                configured
                        ? "Advisories can be published to the configured " + alertDispatcher.channel() + " channel"
                        : "No notification channel is configured on this deployment, so advisories can be "
                                + "downloaded but not pushed");
    }

    /**
     * Publishes one advisory to the configured channel.
     *
     * @param request the same assessment payload the console uses, validated before mapping
     * @return what was sent and whether the channel accepted it
     */
    @PostMapping(path = "/dispatch", produces = MediaType.APPLICATION_JSON_VALUE)
    public AdvisoryDispatchResponse dispatch(@Valid @RequestBody ImpactAssessmentRequest request) {
        AdvisoryLanguage language = AdvisoryLanguage.fromCode(request.language());

        CycloneTrack track = ImpactAssessmentMapper.toTrack(request.track());
        List<InfrastructureAsset> assets = ImpactAssessmentMapper.toAssets(request.assets());
        ImpactReport report = request.evaluationTime() == null
                ? impactService.assess(track, assets, track.endTime(), language)
                : impactService.assess(track, assets, request.evaluationTime(), language);

        String capXml = capAlertWriter.write(report, language, capAlertPublication.options());
        String capIdentifier = capAlertWriter.identifier(report, language);
        String headline = AdvisoryVocabulary.headline(
                report.stormId(),
                report.actionableExposures().size(),
                report.summary().highestRisk(),
                language);

        DispatchResult result = alertDispatcher.dispatch(
                new DispatchRequest(report.stormId(), language, headline, report.advisory(), capXml));

        AdvisoryProvenance provenance = report.advisoryProvenance();
        return new AdvisoryDispatchResponse(
                result.delivered(),
                result.channel(),
                alertDispatcher.configured(),
                report.stormId(),
                language.code(),
                headline,
                report.advisory(),
                provenance.generator(),
                provenance.model(),
                provenance.degraded(),
                capIdentifier,
                result.detail(),
                result.latencyMillis());
    }

    /**
     * Readiness of the outbound channel.
     *
     * @param channel identifier of the channel, for example {@code webhook}
     * @param configured whether the channel can be called
     * @param detail operator-readable explanation of either state
     */
    public record AdvisoryChannelResponse(String channel, boolean configured, String detail) {
    }

    /**
     * The audit record of one publication.
     *
     * @param delivered true only when the channel acknowledged the advisory
     * @param advisory the exact text that was sent, so an operator can see what left the building
     * @param generator which strategy wrote the text that was sent
     * @param capIdentifier the CAP alert identifier, for correlation with a downstream system
     */
    public record AdvisoryDispatchResponse(
            boolean delivered,
            String channel,
            boolean channelConfigured,
            String stormId,
            String language,
            String headline,
            String advisory,
            String generator,
            String model,
            boolean degraded,
            String capIdentifier,
            String detail,
            long latencyMillis) {
    }
}
