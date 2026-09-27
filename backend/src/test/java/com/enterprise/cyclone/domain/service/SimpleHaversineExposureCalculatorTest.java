package com.enterprise.cyclone.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.model.RiskLevel;
import com.enterprise.cyclone.domain.model.SaffirSimpsonCategory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The screening rules, tested at every boundary.
 *
 * <p>This is the class that decides which bridge gets inspected before landfall, so the tests are
 * written against the thresholds themselves rather than against a fixture: each level is exercised
 * just inside and just outside its distance band, and the wind criterion is exercised separately, so
 * that a change to either constant fails a named test rather than quietly reshuffling a demo.
 *
 * <p>Assets are placed due north of the storm centre, which makes the great-circle distance exact
 * (the haversine formula reduces to a meridian arc when the longitude difference is zero), so the
 * intended distance is the actual distance to within floating-point noise. Bands are probed 0.05 nm
 * either side of a threshold — about 90 m, far below the precision of any cyclone fix — because
 * asserting on an exact boundary would test the floating-point implementation rather than the rule.
 */
class SimpleHaversineExposureCalculatorTest {

    private static final SimpleHaversineExposureCalculator CALCULATOR = new SimpleHaversineExposureCalculator();

    private static final Instant FIX_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final double STORM_LATITUDE = 15.0;
    private static final double STORM_LONGITUDE = 85.0;

    /** Distance covered by one degree of latitude on the sphere the calculator uses. */
    private static final double NAUTICAL_MILES_PER_DEGREE =
            new GeoCoordinate(0.0, 0.0).distanceNauticalMilesTo(new GeoCoordinate(1.0, 0.0));

    /** Half-width of the probe either side of a threshold, in nautical miles. */
    private static final double BOUNDARY_PROBE_NM = 0.05;

    /** Documented thresholds, asserted so that a silent change to the model is visible in the build. */
    private static final int HURRICANE_FORCE_KNOTS = 64;
    private static final int DAMAGING_WIND_KNOTS = 50;
    private static final int GALE_FORCE_KNOTS = 34;

    @Test
    void documentedThresholdsMatchTheSeverityScale() {
        assertThat(SaffirSimpsonCategory.CAT1.minWindSpeedKnots())
                .as("hurricane-force threshold is the category 1 floor")
                .isEqualTo(HURRICANE_FORCE_KNOTS);
        assertThat(SaffirSimpsonCategory.TS.minWindSpeedKnots())
                .as("gale-force threshold is the tropical-storm floor")
                .isEqualTo(GALE_FORCE_KNOTS);
    }

    @Test
    void windDecaysExponentiallyOverTheEFoldingDistance() {
        // v(d) = vMax * exp(-d / 75 nm): at 75 nm the model retains 1/e of the maximum wind.
        AssetExposure atZero = exposureFor(100, 0.0);
        AssetExposure atDecayDistance = exposureFor(100, 75.0);

        assertThat(atZero.distanceNauticalMiles()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(atZero.estimatedWindAtAsset()).isEqualTo(100);
        assertThat(atDecayDistance.estimatedWindAtAsset())
                .as("one e-folding distance")
                .isEqualTo((int) Math.round(100 / Math.E));
    }

    @Test
    void modelledWindFollowsTheDocumentedFormulaAtEveryDistance() {
        for (double distance : new double[] {0.0, 10.0, 35.0, 75.0, 120.0, 200.0}) {
            AssetExposure exposure = exposureFor(160, distance);

            assertThat(exposure.estimatedWindAtAsset())
                    .as("wind at %.1f nm", distance)
                    .isEqualTo((int) Math.round(160 * Math.exp(-exposure.distanceNauticalMiles() / 75.0)));
        }
    }

    @Test
    void hurricaneForceByDistanceJustInsideTheThirtyMileBand() {
        AssetExposure exposure = exposureFor(30, 30.0 - BOUNDARY_PROBE_NM);

        assertThat(exposure.distanceNauticalMiles()).isLessThan(30.0);
        assertThat(exposure.estimatedWindAtAsset()).isLessThan(HURRICANE_FORCE_KNOTS);
        assertThat(exposure.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(exposure.rationale()).contains("inside the 30 nm hurricane-force band");
        assertThat(exposure.isActionable()).isTrue();
    }

    @Test
    void justOutsideTheThirtyMileBandFallsToHighWhenWindIsWeak() {
        AssetExposure exposure = exposureFor(30, 30.0 + BOUNDARY_PROBE_NM);

        assertThat(exposure.distanceNauticalMiles()).isGreaterThan(30.0);
        assertThat(exposure.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(exposure.rationale()).contains("inside the 60 nm damaging-wind band");
    }

    @Test
    void galeBandJustInsideOneHundredAndTwentyMilesIsMedium() {
        AssetExposure exposure = exposureFor(20, 120.0 - BOUNDARY_PROBE_NM);

        assertThat(exposure.distanceNauticalMiles()).isLessThan(120.0);
        assertThat(exposure.estimatedWindAtAsset()).isLessThan(GALE_FORCE_KNOTS);
        assertThat(exposure.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(exposure.rationale()).contains("inside the 120 nm gale-force band");
    }

    @Test
    void beyondEveryBandWithWeakWindIsLow() {
        AssetExposure exposure = exposureFor(20, 120.0 + BOUNDARY_PROBE_NM);

        assertThat(exposure.distanceNauticalMiles()).isGreaterThan(120.0);
        assertThat(exposure.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(exposure.rationale()).contains("below every gale-force threshold");
        assertThat(exposure.isActionable()).isFalse();
    }

    @Test
    void windCriterionAloneCanReachCriticalOutsideTheThirtyMileBand() {
        // 100 kt maximum, 30.05 nm away: the distance band is not met but the modelled wind (67 kt) is.
        AssetExposure byWind = exposureFor(100, 30.0 + BOUNDARY_PROBE_NM);

        assertThat(byWind.distanceNauticalMiles()).isGreaterThan(30.0);
        assertThat(byWind.estimatedWindAtAsset()).isGreaterThanOrEqualTo(HURRICANE_FORCE_KNOTS);
        assertThat(byWind.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(byWind.rationale()).contains("reaches hurricane force");
    }

    @Test
    void theMoreSevereOfTheTwoCriteriaDecidesTheLevel() {
        // Same position, opposite causes: distance alone versus wind alone both reach critical.
        AssetExposure byDistance = exposureFor(30, 30.0 - BOUNDARY_PROBE_NM);
        AssetExposure byWind = exposureFor(100, 30.0 + BOUNDARY_PROBE_NM);

        assertThat(byDistance.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(byWind.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(byDistance.rationale()).isNotEqualTo(byWind.rationale());
    }

    @ParameterizedTest
    // Piped delimiter so the expected reasons can contain commas, and asserted in full because the
    // reason is what an operator reads when deciding whether to accept the level.
    @CsvSource(delimiter = '|', value = {
        // maximum wind | distance | expected level | expected reason
        "128 | 70.0 | HIGH   | modelled wind of 50 kt at the asset reaches the 50 kt damaging-wind threshold",
        "124 | 70.0 | MEDIUM | modelled wind of 49 kt at the asset reaches gale force (34 kt)",
        "120 | 100.0 | MEDIUM | asset lies 100 nm from the storm centre, inside the 120 nm gale-force band",
    })
    void damagingWindThresholdSitsAtFiftyKnots(
            int maximumWindKnots, double distanceNm, RiskLevel expected, String expectedReason) {
        AssetExposure exposure = exposureFor(maximumWindKnots, distanceNm);

        assertThat(exposure.riskLevel()).isEqualTo(expected);
        assertThat(exposure.rationale()).isEqualTo(expectedReason);
    }

    @Test
    void evaluatingBetweenFixesUsesTheInterpolatedIntensity() {
        // Two fixes, 100 kt then 60 kt; at the midpoint the storm carries 80 kt, which the wind
        // field must decay from rather than from either published value.
        CycloneTrack track = new CycloneTrack("INTERP", List.of(
                new CycloneTrackPoint(STORM_LATITUDE, STORM_LONGITUDE, 100, 970, FIX_TIME),
                new CycloneTrackPoint(STORM_LATITUDE, STORM_LONGITUDE, 60, 990, FIX_TIME.plusSeconds(21_600))));
        Instant midpoint = FIX_TIME.plusSeconds(10_800);

        List<AssetExposure> exposures =
                CALCULATOR.calculate(track, List.of(assetAtDistance(0.0, "CENTRE")), midpoint);

        assertThat(exposures).singleElement().satisfies(exposure ->
                assertThat(exposure.estimatedWindAtAsset()).isEqualTo(80));
    }

    @Test
    void resultsAreOrderedBySeverityThenDistance() {
        CycloneTrack track = new CycloneTrack("ORDER", List.of(
                new CycloneTrackPoint(STORM_LATITUDE, STORM_LONGITUDE, 100, 970, FIX_TIME)));
        List<InfrastructureAsset> assets = List.of(
                assetAtDistance(200.0, "FAR-LOW"),
                assetAtDistance(40.0, "NEAR-HIGH"),
                assetAtDistance(10.0, "CENTRE-CRITICAL"),
                assetAtDistance(100.0, "MID-MEDIUM"),
                assetAtDistance(45.0, "NEARER-HIGH"));

        List<AssetExposure> exposures = CALCULATOR.calculate(track, assets, FIX_TIME);

        assertThat(exposures).extracting(exposure -> exposure.asset().id())
                .containsExactly("CENTRE-CRITICAL", "NEAR-HIGH", "NEARER-HIGH", "MID-MEDIUM", "FAR-LOW");
    }

    @Test
    void rejectsAnEmptyAssetCollection() {
        CycloneTrack track = new CycloneTrack("EMPTY", List.of(
                new CycloneTrackPoint(STORM_LATITUDE, STORM_LONGITUDE, 60, 990, FIX_TIME)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CALCULATOR.calculate(track, List.of(), FIX_TIME))
                .withMessageContaining("at least one asset");
    }

    @Test
    void rejectsNullArguments() {
        CycloneTrack track = new CycloneTrack("NULLS", List.of(
                new CycloneTrackPoint(STORM_LATITUDE, STORM_LONGITUDE, 60, 990, FIX_TIME)));
        List<InfrastructureAsset> assets = List.of(assetAtDistance(10.0, "A1"));

        assertThatNullPointerException().isThrownBy(() -> CALCULATOR.calculate(null, assets, FIX_TIME));
        assertThatNullPointerException().isThrownBy(() -> CALCULATOR.calculate(track, null, FIX_TIME));
        assertThatNullPointerException().isThrownBy(() -> CALCULATOR.calculate(track, assets, null));
    }

    /** Assesses one asset against a single-fix storm of the given intensity. */
    private static AssetExposure exposureFor(int maximumWindKnots, double distanceNm) {
        CycloneTrack track = new CycloneTrack("SINGLE", List.of(
                new CycloneTrackPoint(STORM_LATITUDE, STORM_LONGITUDE, maximumWindKnots, 990, FIX_TIME)));

        return CALCULATOR.calculate(track, List.of(assetAtDistance(distanceNm, "A1")), FIX_TIME).get(0);
    }

    /** Places an asset due north of the storm centre, so the distance is an exact meridian arc. */
    private static InfrastructureAsset assetAtDistance(double distanceNm, String id) {
        double latitude = STORM_LATITUDE + (distanceNm / NAUTICAL_MILES_PER_DEGREE);
        return new InfrastructureAsset(
                id, "Asset " + id, AssetType.POWER_GRID, new GeoCoordinate(latitude, STORM_LONGITUDE));
    }
}
