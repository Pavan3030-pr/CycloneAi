package com.enterprise.cyclone.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.model.RiskLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The advisory template, in every language it can be issued in.
 *
 * <p>The English test asserts the output in full rather than by fragment, because this text is the
 * established output of the platform: operators, briefings and downstream systems have seen it. A
 * change to it should be a deliberate edit to this test, never a side effect of a refactor.
 *
 * <p>The Hindi and Telugu tests assert the same facts appear with localised labels, that the storm
 * identifier and the model's limitations survive translation, and that the English risk labels are
 * not silently reused — a translated advisory that still says "CRITICAL" has not been translated.
 */
class DeterministicAdvisoryGeneratorTest {

    private static final DeterministicAdvisoryGenerator GENERATOR = new DeterministicAdvisoryGenerator();

    @Test
    void englishOutputIsUnchanged() {
        String advisory = GENERATOR.generate(context(), AdvisoryLanguage.EN);

        assertThat(advisory).isEqualTo("""
                CYCLONE IMPACT ADVISORY - IO-DEMO-01
                Valid at 2023-12-05 06:00 UTC
                Storm centre: latitude 15.7500, longitude 80.3000 (GeoJSON position 80.3000,15.7500)
                Intensity: 48 kt (Tropical Storm), central pressure 994 mb
                Assets assessed: 2
                Pre-landfall action required for: 1 asset(s)
                  CRITICAL 1 asset(s)
                  HIGH     0 asset(s)
                  MEDIUM   0 asset(s)
                  LOW      1 asset(s)
                Nearest asset: 13 nm from the centre; peak modelled wind at an asset: 40 kt
                Priority assets:
                  1. [CRITICAL] Bapatla coastal shelter (MEDICAL_SHELTER) - 13 nm, 40 kt: asset lies 13 nm \
                from the storm centre, inside the 30 nm hurricane-force band
                  2. [LOW] Chennai bypass (NH-32) (ARTERIAL_ROAD) - 151 nm, 6 kt: modelled wind of 6 kt at \
                the asset and a distance of 151 nm stay below every gale-force threshold
                Basis: linear interpolation between published fixes with an exponential wind-field decay \
                (75 nm e-folding). Terrain, gust factor, quadrant asymmetry and forecast positional \
                uncertainty are not modelled. Cross-check with official NHC/JTWC products before \
                operational use.""");
    }

    @Test
    void englishOutputEndsWithALineFeedAndNotThePlatformSeparator() {
        String advisory = GENERATOR.generate(context(), AdvisoryLanguage.EN);

        assertThat(advisory).doesNotContain("\r\n");
        assertThat(advisory.lines()).hasSize(15);
        assertThat(advisory).doesNotEndWith("\n");
    }

    @Test
    void hindiAdvisoryIsLocalisedAndCarriesTheFacts() {
        String advisory = GENERATOR.generate(context(), AdvisoryLanguage.HI);

        assertThat(advisory)
                .startsWith("चक्रवात प्रभाव परामर्श - IO-DEMO-01")
                .contains("48 kt")
                .contains("994 mb")
                .contains("अति गंभीर")
                .contains("मध्यम")
                .contains("Bapatla coastal shelter")
                .contains("75 nm");
        assertThat(advisory)
                .as("translated risk labels must not fall back to the enum name")
                .doesNotContain("CRITICAL 1");
    }

    @Test
    void teluguAdvisoryIsLocalisedAndCarriesTheFacts() {
        String advisory = GENERATOR.generate(context(), AdvisoryLanguage.TE);

        assertThat(advisory)
                .startsWith("తుఫాను ప్రభావ హెచ్చరిక - IO-DEMO-01")
                .contains("48 kt")
                .contains("994 mb")
                .contains("అత్యవసర")
                .contains("Bapatla coastal shelter");
        assertThat(advisory).doesNotContain("CRITICAL 1");
    }

    @Test
    void everyLanguageStatesTheModelsOwnLimitations() {
        for (AdvisoryLanguage language : AdvisoryLanguage.values()) {
            String advisory = GENERATOR.generate(context(), language);

            assertThat(advisory)
                    .as("limitations must survive translation in %s", language.code())
                    .contains("75 nm")
                    .contains("NHC/JTWC");
        }
    }

    @Test
    void languageCodesParseLeniently() {
        assertThat(AdvisoryLanguage.fromCode("HI")).isEqualTo(AdvisoryLanguage.HI);
        assertThat(AdvisoryLanguage.parse("te-IN")).contains(AdvisoryLanguage.TE);
        assertThat(AdvisoryLanguage.fromCode("fr")).as("unsupported languages fall back rather than fail")
                .isEqualTo(AdvisoryLanguage.EN);
        assertThat(AdvisoryLanguage.fromCode(null)).isEqualTo(AdvisoryLanguage.EN);
    }

    @Test
    void longAssetListsAreSummarisedRatherThanTruncatedSilently() {
        List<AssetExposure> exposures = new ArrayList<>(context().exposures());
        for (int index = 0; index < 6; index++) {
            exposures.add(new AssetExposure(
                    new InfrastructureAsset(
                            "EXTRA-" + index,
                            "Extra asset " + index,
                            AssetType.POWER_GRID,
                            new GeoCoordinate(16.0 + (index * 0.1), 81.0)),
                    150.0 + index,
                    8,
                    RiskLevel.LOW,
                    "modelled wind of 8 kt at the asset and a distance of 155 nm stay below every "
                            + "gale-force threshold"));
        }
        AdvisoryContext wide = new AdvisoryContext(
                "IO-DEMO-01",
                context().evaluatedAt(),
                context().stormPosition(),
                ImpactSummary.of(exposures),
                exposures);

        String advisory = GENERATOR.generate(wide, AdvisoryLanguage.EN);

        assertThat(advisory)
                .as("five assets named, the remainder accounted for by count")
                .contains("... and 3 further asset(s) at lower exposure")
                .contains("Priority assets:");
        assertThat(advisory.lines().filter(line -> line.startsWith("  ") && line.contains(". [")))
                .hasSize(DeterministicAdvisoryGenerator.MAX_LISTED_ASSETS);
    }

    /** The demonstration storm's assessment shape, at its mid-interval evaluation instant. */
    static AdvisoryContext context() {
        CycloneTrackPoint storm = new CycloneTrackPoint(
                15.75, 80.30, 48, 994, Instant.parse("2023-12-05T06:00:00Z"));

        List<AssetExposure> exposures = List.of(
                new AssetExposure(
                        new InfrastructureAsset(
                                "DEMO-SHELTER-BAPATLA",
                                "Bapatla coastal shelter",
                                AssetType.MEDICAL_SHELTER,
                                new GeoCoordinate(15.9, 80.47)),
                        13.0,
                        40,
                        RiskLevel.CRITICAL,
                        "asset lies 13 nm from the storm centre, inside the 30 nm hurricane-force band"),
                new AssetExposure(
                        new InfrastructureAsset(
                                "DEMO-ROAD-CHENNAI",
                                "Chennai bypass (NH-32)",
                                AssetType.ARTERIAL_ROAD,
                                new GeoCoordinate(12.9, 80.15)),
                        151.0,
                        6,
                        RiskLevel.LOW,
                        "modelled wind of 6 kt at the asset and a distance of 151 nm stay below every "
                                + "gale-force threshold"));

        return new AdvisoryContext(
                "IO-DEMO-01",
                storm.timestamp(),
                storm,
                ImpactSummary.of(exposures),
                exposures);
    }
}
