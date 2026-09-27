package com.enterprise.cyclone.adapter.out.ai;

import com.enterprise.cyclone.application.AdvisoryContext;
import com.enterprise.cyclone.application.AdvisoryLanguage;
import com.enterprise.cyclone.application.AdvisoryVocabulary;
import com.enterprise.cyclone.application.DeterministicAdvisoryGenerator;
import com.enterprise.cyclone.application.port.LocalizedAdvisoryGenerator;
import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.RiskLevel;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Writes advisories with a language model, and refuses to publish text that fails the domain checks.
 *
 * <p>Two responsibilities, both deliberate. The first is the prompt: every number the model is
 * allowed to mention is enumerated from {@link AdvisoryContext}, and it is told in as many words not
 * to invent population figures, timings, depths or assets. A model writing about a cyclone will
 * happily produce a plausible-sounding casualty estimate, and a fabricated number in an official
 * advisory is worse than a dull one.
 *
 * <p>The second is the validation gate. The response must be substantial, must name the storm it is
 * about, and must be plain text. Anything else raises, and
 * {@link com.enterprise.cyclone.application.AdvisoryComposer} then publishes the deterministic
 * advisory with the failure recorded in provenance — so a hallucination or a truncated completion
 * degrades the wording, never the warning.
 */
public final class GeminiAdvisoryGenerator implements LocalizedAdvisoryGenerator {

    private static final int MINIMUM_ACCEPTABLE_CHARACTERS = 60;
    private static final int MAXIMUM_ACCEPTABLE_CHARACTERS = 4000;
    private static final int MAXIMUM_LISTED_ASSETS = DeterministicAdvisoryGenerator.MAX_LISTED_ASSETS;

    private final GeminiAdvisoryClient client;

    public GeminiAdvisoryGenerator(GeminiAdvisoryClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public String generate(AdvisoryContext context, AdvisoryLanguage language) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(language, "language must not be null");

        String instruction = prompt(context, language);
        String response = client.reason(instruction, List.of());
        return validate(response, context);
    }

    /**
     * Renders the prompt.
     *
     * <p>Package-private so a test can assert that the facts handed to the model are exactly the facts
     * of the assessment, and that no unpublished number, coordinate or identifier is included.
     */
    static String prompt(AdvisoryContext context, AdvisoryLanguage language) {
        CycloneTrackPoint storm = context.stormPosition();
        StringBuilder prompt = new StringBuilder(2048);

        prompt.append("Write the cyclone impact advisory for the assessment below, in ")
                .append(language.displayName())
                .append(" (").append(language.code()).append(").\n")
                .append("Use that language for all prose. Keep storm identifiers, asset identifiers and units ")
                .append("(kt, mb, nm, UTC) exactly as given.\n\n")
                .append("ASSESSMENT FACTS (the only facts you may state):\n")
                .append("- Storm identifier: ").append(context.stormId()).append('\n')
                .append("- Valid at: ").append(AdvisoryVocabulary.validAt(context.evaluatedAt(), AdvisoryLanguage.EN))
                .append('\n')
                .append("- Storm centre: ").append(String.format(
                        Locale.ROOT, "%.4f, %.4f", storm.latitude(), storm.longitude()))
                .append(" (interpolated between published fixes, not an observation)\n")
                .append("- Intensity: ").append(storm.windSpeedKnots()).append(" kt, ")
                .append(AdvisoryVocabulary.categoryName(context.category(), AdvisoryLanguage.EN))
                .append(", central pressure ").append(storm.centralPressureMb()).append(" mb\n")
                .append("- Assets assessed: ").append(context.summary().totalAssets()).append('\n')
                .append("- Assets requiring pre-landfall action: ").append(actionableCount(context)).append('\n')
                .append("- Counts by assessed level: ");
        for (RiskLevel level : RiskLevel.values()) {
            prompt.append(level.name()).append('=').append(context.summary().countOf(level)).append(' ');
        }
        prompt.append('\n')
                .append("- Nearest asset: ").append(Math.round(context.summary().nearestDistanceNauticalMiles()))
                .append(" nm from the centre\n")
                .append("- Peak modelled wind at an asset: ").append(context.summary().peakEstimatedWindKnots())
                .append(" kt\n")
                .append("\nMOST EXPOSED ASSETS:\n");

        List<AssetExposure> exposures = context.exposures();
        int listed = Math.min(exposures.size(), MAXIMUM_LISTED_ASSETS);
        for (int index = 0; index < listed; index++) {
            AssetExposure exposure = exposures.get(index);
            prompt.append(index + 1).append(". ")
                    .append(exposure.riskLevel().name()).append(" — ")
                    .append(exposure.asset().name())
                    .append(" [").append(exposure.asset().assetType().name()).append("]")
                    .append(", ").append(Math.round(exposure.distanceNauticalMiles())).append(" nm, ")
                    .append(exposure.estimatedWindAtAsset()).append(" kt")
                    .append(". Basis: ").append(exposure.rationale())
                    .append('\n');
        }

        prompt.append("""
                
                REQUIREMENTS:
                - Structure, in this order: one headline line naming the storm; the storm's assessed state at \
                the validity time; the assets requiring action with their distances and modelled wind; one \
                short instruction line for the district control room.
                - State only the numbers above. Do not invent or estimate population, deaths, injuries, \
                rainfall, storm surge heights, road names, building damage, landfall time or any asset that \
                is not listed.
                - Do not recompute, round differently or contradict any supplied number, distance or level.
                - Do not claim the storm will or will not make landfall: the model screens exposure, it does \
                not forecast track.
                - Plain text only: no markdown, no bullet characters, no headings, no code fences, no \
                greeting, no signature.
                - At most 180 words.
                - End with this sentence, translated into the same language and otherwise unchanged:
                """);
        prompt.append('"').append(AdvisoryVocabulary.basis(language)).append('"');

        return prompt.toString();
    }

    /**
     * Rejects unusable model output.
     *
     * @throws IllegalStateException if the text is empty, implausibly short or long, or does not name
     *         the storm it is supposed to describe
     */
    static String validate(String response, AdvisoryContext context) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("the model returned no text");
        }

        String text = stripCodeFences(response.strip());
        if (text.length() < MINIMUM_ACCEPTABLE_CHARACTERS) {
            throw new IllegalStateException(
                    "the model returned only " + text.length() + " characters, which is not an advisory");
        }
        if (text.length() > MAXIMUM_ACCEPTABLE_CHARACTERS) {
            throw new IllegalStateException(
                    "the model returned " + text.length() + " characters, beyond the accepted length");
        }
        if (!text.toLowerCase(Locale.ROOT).contains(context.stormId().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "the model output never names storm " + context.stormId() + ", so it cannot be published");
        }
        return text;
    }

    /** Removes a markdown fence if the model wrapped its answer in one despite being told not to. */
    private static String stripCodeFences(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text.replace("```", "").strip();
        }
        String body = text.substring(firstNewline + 1);
        int lastFence = body.lastIndexOf("```");
        if (lastFence >= 0) {
            body = body.substring(0, lastFence);
        }
        return body.strip();
    }

    private static int actionableCount(AdvisoryContext context) {
        return (int) context.exposures().stream().filter(AssetExposure::isActionable).count();
    }
}
