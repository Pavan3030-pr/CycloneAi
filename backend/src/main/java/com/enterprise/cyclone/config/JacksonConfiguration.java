package com.enterprise.cyclone.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bounds the shape of every JSON document the API will parse.
 *
 * <p>Jackson's defaults are generous because they favour compatibility. For a public API that
 * accepts nested objects from untrusted callers they are too generous: a deeply nested or enormous
 * document is a cheap way to exhaust memory, and a chunked request declares no
 * {@code Content-Length} for the size filter to check. These limits are enforced by the parser
 * itself, so they apply to bodies written by hand as well as to bound requests.
 */
@Configuration
public class JacksonConfiguration {

    private static final int MAX_NESTING_DEPTH = 32;
    private static final int MAX_STRING_LENGTH = 262_144;
    private static final int MAX_NUMBER_LENGTH = 64;
    private static final int MAX_NAME_LENGTH = 256;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer hardenJsonParser() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxNestingDepth(MAX_NESTING_DEPTH)
                .maxStringLength(MAX_STRING_LENGTH)
                .maxNumberLength(MAX_NUMBER_LENGTH)
                .maxNameLength(MAX_NAME_LENGTH)
                .build();

        return builder -> builder.postConfigurer(mapper -> mapper.getFactory().setStreamReadConstraints(constraints));
    }
}
