package com.enterprise.cyclone.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the hardening filters with explicit order.
 *
 * <p>The filters are built here rather than annotated as components so that Spring Boot does not
 * also register them implicitly, which would apply each one twice. The order values place
 * correlation first so every later log line carries it, and everything else after the Spring
 * Security chain (order {@code -100}) so that filters needing the authenticated principal find it.
 */
@Configuration
public class FilterRegistration {

    private static final int SECURITY_FILTER_ORDER = -100;

    /**
     * Outermost filter: establishes the correlation id for the whole request, including requests
     * that Spring Security rejects.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(SECURITY_FILTER_ORDER - 10);
        return registration;
    }

    /**
     * Runs immediately inside the security chain, so an oversized anonymous body is refused before
     * any parsing happens.
     */
    @Bean
    public FilterRegistrationBean<PayloadSizeLimitFilter> payloadSizeLimitFilter(
            SecurityProperties properties, ProblemResponseWriter problemWriter) {
        FilterRegistrationBean<PayloadSizeLimitFilter> registration =
                new FilterRegistrationBean<>(new PayloadSizeLimitFilter(properties, problemWriter));
        registration.setOrder(SECURITY_FILTER_ORDER + 5);
        return registration;
    }

    /**
     * Runs before the guarded endpoints but after authentication, so the bucket can be keyed by
     * principal.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            SecurityProperties properties, ProblemResponseWriter problemWriter) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(properties.rateLimit(), problemWriter));
        registration.setOrder(SECURITY_FILTER_ORDER + 15);
        registration.setEnabled(properties.rateLimit().enabled());
        return registration;
    }

    /**
     * Innermost of the custom filters: logs the completed request once the status is known.
     */
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registration =
                new FilterRegistrationBean<>(new RequestLoggingFilter());
        registration.setOrder(SECURITY_FILTER_ORDER + 20);
        return registration;
    }
}
