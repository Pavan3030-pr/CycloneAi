package com.enterprise.cyclone.config;

import com.enterprise.cyclone.adapter.out.feed.NoaaGeoJsonTrackParser;
import com.enterprise.cyclone.adapter.out.registry.InMemoryAssetRegistry;
import com.enterprise.cyclone.application.AssetRegistryService;
import com.enterprise.cyclone.application.CycloneImpactService;
import com.enterprise.cyclone.application.DeterministicAdvisoryGenerator;
import com.enterprise.cyclone.application.port.AdvisoryGenerator;
import com.enterprise.cyclone.application.port.AssetRegistry;
import com.enterprise.cyclone.domain.service.ExposureCalculator;
import com.enterprise.cyclone.domain.service.SimpleHaversineExposureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free core into the Spring context.
 *
 * <p>Domain and application classes carry no Spring annotations on purpose, so this class is the
 * only place that knows how they are obtained. Swapping the exposure model, replacing the template
 * advisory with a model-backed {@link AdvisoryGenerator}, or backing the asset registry with a
 * database is therefore a change to a bean method, with no edit to any class under {@code domain} or
 * {@code application}.
 */
@Configuration
public class CycloneImpactConfiguration {

    @Bean
    public ExposureCalculator exposureCalculator() {
        return new SimpleHaversineExposureCalculator();
    }

    @Bean
    public AdvisoryGenerator advisoryGenerator() {
        return new DeterministicAdvisoryGenerator();
    }

    @Bean
    public CycloneImpactService cycloneImpactService(
            ExposureCalculator exposureCalculator, AdvisoryGenerator advisoryGenerator) {
        return new CycloneImpactService(exposureCalculator, advisoryGenerator);
    }

    @Bean
    public AssetRegistry assetRegistry() {
        return new InMemoryAssetRegistry();
    }

    @Bean
    public AssetRegistryService assetRegistryService(AssetRegistry assetRegistry) {
        return new AssetRegistryService(assetRegistry);
    }

    /**
     * Injected rather than read from {@link Clock#systemUTC()} directly, so that token expiry and any
     * future time-dependent decision can be driven deterministically in tests.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public NoaaGeoJsonTrackParser noaaGeoJsonTrackParser(ObjectMapper objectMapper) {
        return new NoaaGeoJsonTrackParser(objectMapper);
    }
}
