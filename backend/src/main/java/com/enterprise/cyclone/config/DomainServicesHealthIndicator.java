package com.enterprise.cyclone.config;

import com.enterprise.cyclone.application.AdvisoryComposer;
import com.enterprise.cyclone.application.AdvisoryContext;
import com.enterprise.cyclone.application.ImpactSummary;
import com.enterprise.cyclone.application.port.AdvisoryGenerator;
import com.enterprise.cyclone.domain.model.AssetExposure;
import com.enterprise.cyclone.domain.model.AssetType;
import com.enterprise.cyclone.domain.model.CycloneTrack;
import com.enterprise.cyclone.domain.model.CycloneTrackPoint;
import com.enterprise.cyclone.domain.model.GeoCoordinate;
import com.enterprise.cyclone.domain.model.InfrastructureAsset;
import com.enterprise.cyclone.domain.service.ExposureCalculator;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Proves the core is alive, not merely that the process started.
 *
 * <p>A bean-presence check would pass while the exposure model was broken, so this indicator runs a
 * real two-fix assessment through the actual collaborators: interpolation, exposure calculation,
 * summary aggregation and advisory rendering. If any of them is misfiring, the health endpoint says
 * so before a user discovers it. The objects used are ordinary domain values built at startup, not a
 * stubbed code path, so the probe exercises the same code a request does.
 *
 * <p>The self-test deliberately renders with the deterministic generator and never with the language
 * model. A probe that called a third-party API would turn someone else's bad minute into a failed
 * health check, and an orchestrator restarting this service because Gemini was slow is exactly the
 * wrong response to a slow model. Which strategy is active is reported as a detail, so the state is
 * still visible at a glance.
 */
@Component
public class DomainServicesHealthIndicator implements HealthIndicator {

    private static final CycloneTrack SELF_TEST_TRACK = new CycloneTrack("SELF-TEST", List.of(
            new CycloneTrackPoint(15.0, 85.0, 45, 998, Instant.parse("2026-01-01T00:00:00Z")),
            new CycloneTrackPoint(15.6, 85.8, 75, 972, Instant.parse("2026-01-01T06:00:00Z"))));

    private static final InfrastructureAsset SELF_TEST_ASSET = new InfrastructureAsset(
            "self-test", "Domain self-test asset", AssetType.MEDICAL_SHELTER, new GeoCoordinate(15.2, 85.3));

    private final ExposureCalculator exposureCalculator;
    private final AdvisoryGenerator advisoryGenerator;
    private final AdvisoryComposer advisoryComposer;

    public DomainServicesHealthIndicator(
            ExposureCalculator exposureCalculator,
            AdvisoryGenerator advisoryGenerator,
            AdvisoryComposer advisoryComposer) {
        this.exposureCalculator = exposureCalculator;
        this.advisoryGenerator = advisoryGenerator;
        this.advisoryComposer = advisoryComposer;
    }

    @Override
    public Health health() {
        long startedAt = System.nanoTime();
        try {
            List<AssetExposure> exposures = exposureCalculator.calculate(
                    SELF_TEST_TRACK, List.of(SELF_TEST_ASSET), SELF_TEST_TRACK.endTime());
            if (exposures.isEmpty()) {
                return Health.down().withDetail("selfTest", "exposure calculation returned no result").build();
            }

            ImpactSummary summary = ImpactSummary.of(exposures);
            AdvisoryContext context = new AdvisoryContext(
                    SELF_TEST_TRACK.stormId(), SELF_TEST_TRACK.endTime(),
                    SELF_TEST_TRACK.points().get(0), summary, exposures);
            String advisory = advisoryGenerator.generate(context);
            if (advisory.isBlank()) {
                return Health.down().withDetail("selfTest", "advisory generation returned no text").build();
            }

            long durationMicroseconds = (System.nanoTime() - startedAt) / 1_000L;
            return Health.up()
                    .withDetail("exposureCalculator", exposureCalculator.getClass().getSimpleName())
                    .withDetail("advisoryGenerator", advisoryGenerator.getClass().getSimpleName())
                    .withDetail("advisoryStrategy", advisoryComposer.modelEnabled() ? "gemini" : "deterministic")
                    .withDetail("advisoryModel", advisoryComposer.modelEnabled() ? advisoryComposer.modelName() : "none")
                    .withDetail("selfTest", "interpolation, exposure, summary and advisory pipeline executed")
                    .withDetail("selfTestDurationMicroseconds", durationMicroseconds)
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception)
                    .withDetail("exposureCalculator", exposureCalculator.getClass().getSimpleName())
                    .withDetail("advisoryGenerator", advisoryGenerator.getClass().getSimpleName())
                    .build();
        }
    }
}
