package com.enterprise.cyclone.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Clients for the two outbound calls this application makes: the model that writes advisories and the
 * channel that publishes them.
 *
 * <p>Both are configured here with explicit timeouts, because the default is no read timeout at all
 * and an advisory request that hangs holds a request thread and delays a warning. The read timeouts
 * are short on purpose: a beautifully written advisory that arrives after the briefing has started is
 * worth less than a plain one that arrives now, which is exactly why the deterministic generator sits
 * behind both of these calls.
 *
 * <p>Both clients are also constructed from the injected {@link RestClient.Builder}, so an
 * environment that needs a proxy, mutual TLS or an interceptor can add it in one place.
 */
@Configuration
public class OutboundHttpConfiguration {

    @Bean(name = "geminiRestClient")
    public RestClient geminiRestClient(RestClient.Builder builder, AiProperties properties) {
        AiProperties.Advisory advisory = properties.advisory();
        return builder.clone()
                .baseUrl(properties.gemini().endpoint())
                .requestFactory(requestFactory(advisory.connectTimeout(), advisory.readTimeout()))
                .build();
    }

    @Bean(name = "disseminationRestClient")
    public RestClient disseminationRestClient(RestClient.Builder builder, DisseminationProperties properties) {
        DisseminationProperties.Webhook webhook = properties.webhook();
        return builder.clone()
                .requestFactory(requestFactory(webhook.connectTimeout(), webhook.readTimeout()))
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
