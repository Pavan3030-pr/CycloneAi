package com.enterprise.cyclone.config;

import com.enterprise.cyclone.adapter.out.ai.GeminiAdvisoryGenerator;
import com.enterprise.cyclone.adapter.out.ai.RestGeminiAdvisoryClient;
import com.enterprise.cyclone.adapter.out.feed.NoaaGeoJsonTrackParser;
import com.enterprise.cyclone.adapter.out.notify.RestWebhookAlertDispatcher;
import com.enterprise.cyclone.adapter.out.registry.InMemoryAssetRegistry;
import com.enterprise.cyclone.application.AdvisoryComposer;
import com.enterprise.cyclone.application.AssetRegistryService;
import com.enterprise.cyclone.application.CapAlertWriter;
import com.enterprise.cyclone.application.CycloneImpactService;
import com.enterprise.cyclone.application.DeterministicAdvisoryGenerator;
import com.enterprise.cyclone.application.port.AdvisoryGenerator;
import com.enterprise.cyclone.application.port.AlertDispatcher;
import com.enterprise.cyclone.application.port.AssetRegistry;
import com.enterprise.cyclone.application.port.LocalizedAdvisoryGenerator;
import com.enterprise.cyclone.domain.service.ExposureCalculator;
import com.enterprise.cyclone.domain.service.SimpleHaversineExposureCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires the framework-free core into the Spring context.
 *
 * <p>Domain and application classes carry no Spring annotations on purpose, so this class is the
 * only place that knows how they are obtained. Swapping the exposure model, replacing the template
 * advisory with a model-backed {@link AdvisoryGenerator}, or backing the asset registry with a
 * database is therefore a change to a bean method, with no edit to any class under {@code domain} or
 * {@code application}.
 *
 * <p>The advisory wiring follows that rule exactly. The Gemini generator is constructed only when the
 * configuration can actually support it, and it is handed to {@link AdvisoryComposer} as an optional
 * collaborator rather than being required: a deployment with no API key starts, assesses and warns,
 * and simply records in provenance that the deterministic generator wrote the text.
 */
@Configuration
@EnableConfigurationProperties({AiProperties.class, DisseminationProperties.class})
public class CycloneImpactConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CycloneImpactConfiguration.class);

    @Bean
    public ExposureCalculator exposureCalculator() {
        return new SimpleHaversineExposureCalculator();
    }

    /**
     * The always-available template generator. Also the advisory fallback, which is why it is a
     * first-class bean rather than a detail of the composer.
     *
     * <p>Declared as its concrete type because it implements both advisory ports: the single-language
     * {@link AdvisoryGenerator} the domain self-test uses, and the language-aware
     * {@link LocalizedAdvisoryGenerator} the composer falls back to.
     */
    @Bean
    public DeterministicAdvisoryGenerator advisoryGenerator() {
        return new DeterministicAdvisoryGenerator();
    }

    /**
     * Decides between the language model and the template, and records which one ran.
     *
     * <p>Availability is decided once, at startup, from configuration rather than per request: the
     * model is either part of this deployment or it is not, and an operator should be able to see
     * which by looking at the logs, the health endpoint and the console rather than by inferring it
     * from the wording of an advisory.
     */
    @Bean
    public AdvisoryComposer advisoryComposer(
            DeterministicAdvisoryGenerator advisoryGenerator,
            AiProperties properties,
            @Qualifier("geminiRestClient") RestClient geminiRestClient) {

        AiProperties.Advisory advisory = properties.advisory();
        AiProperties.Gemini gemini = properties.gemini();
        boolean modelEnabled = advisory.modelRequested() && gemini.configured();

        LocalizedAdvisoryGenerator modelGenerator = null;
        if (modelEnabled) {
            modelGenerator = new GeminiAdvisoryGenerator(new RestGeminiAdvisoryClient(
                    geminiRestClient,
                    gemini.apiKey(),
                    gemini.model(),
                    gemini.maxOutputTokens(),
                    gemini.temperature()));
            LOG.info("Advisory generation: Gemini model {} at {} with a {} read timeout",
                    gemini.model(), gemini.endpoint(), advisory.readTimeout());
        } else if (advisory.modelRequired()) {
            LOG.warn("app.ai.advisory.mode=gemini but no Gemini API key is configured "
                    + "(set APP_GEMINI_API_KEY), so advisories will be produced by the deterministic generator "
                    + "and reported as degraded.");
        } else {
            LOG.info("Advisory generation: deterministic template generator; "
                    + "set APP_GEMINI_API_KEY to enable model-written advisories");
        }

        return new AdvisoryComposer(
                advisoryGenerator,
                modelGenerator,
                modelEnabled,
                advisory.modelRequired(),
                gemini.model());
    }

    @Bean
    public CycloneImpactService cycloneImpactService(
            ExposureCalculator exposureCalculator, AdvisoryComposer advisoryComposer) {
        return new CycloneImpactService(exposureCalculator, advisoryComposer);
    }

    /** Renders CAP 1.2 alerts; pure and stateless, so one instance serves every request. */
    @Bean
    public CapAlertWriter capAlertWriter() {
        return new CapAlertWriter();
    }

    /**
     * The outbound notification channel. Constructed even when unconfigured, because the console needs
     * to be able to say so rather than hide the capability entirely.
     */
    @Bean
    public AlertDispatcher alertDispatcher(
            @Qualifier("disseminationRestClient") RestClient disseminationRestClient,
            DisseminationProperties properties) {
        return new RestWebhookAlertDispatcher(disseminationRestClient, properties.webhook());
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
