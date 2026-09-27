package com.enterprise.cyclone.application;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.model.RiskLevel;
import java.time.Instant;
import java.util.List;

/**
 * The demonstration assessment, as test data.
 *
 * <p>Shared rather than duplicated so that the advisory, CAP and model-prompt tests all describe the
 * same storm with the same numbers as the one-click demo. A fixture that drifted from the demo would
 * let the suite pass while the thing a judge actually runs was broken.
 */
public final class DemoAssessmentFixture {

    /** The storm the demo loads, at its mid-interval evaluation instant. */
    public static final String STORM_ID = "IO-DEMO-01";

    public static final Instant EVALUATION_TIME = Instant.parse("2023-12-05T06:00:00Z");

    private DemoAssessmentFixture() {
    }

    /** Storm centre at the evaluation instant: 15.75°N 80.30°E, 48 kt, 994 mb. */
    public static CycloneTrackPoint stormPosition() {
        return new CycloneTrackPoint(15.75, 80.30, 48, 994, EVALUATION_TIME);
    }

    /** A shelter inside the hurricane-force band and a highway span far outside it. */
    public static List<AssetExposure> exposures() {
        return List.of(
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
    }

    /** The fact sheet handed to an advisory generator. */
    public static AdvisoryContext context() {
        return new AdvisoryContext(
                STORM_ID,
                EVALUATION_TIME,
                stormPosition(),
                ImpactSummary.of(exposures()),
                exposures());
    }

    /** A complete report with the supplied advisory text and provenance. */
    public static ImpactReport report(String advisory, AdvisoryProvenance provenance) {
        return new ImpactReport(
                STORM_ID,
                EVALUATION_TIME,
                stormPosition(),
                ImpactSummary.of(exposures()),
                advisory,
                provenance,
                exposures());
    }
}
