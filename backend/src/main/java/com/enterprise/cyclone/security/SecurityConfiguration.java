package com.enterprise.cyclone.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless, token-based security for the API.
 *
 * <p>The shape of the policy: nothing is reachable without a token except the token endpoint and the
 * health probe; assessment is available to every authenticated role because it is a read-shaped
 * query; mutating the asset registry needs an analyst; operational endpoints need an administrator.
 * Everything unmatched falls through to {@code authenticated()}, so a new endpoint is protected by
 * default rather than accidentally public.
 *
 * <p>CSRF is disabled because the API is stateless and authorised by a bearer header, which a
 * browser does not attach automatically, so there is no ambient authority for a cross-site request
 * to abuse.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);
    private static final String JWT_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final String[] API_DOCUMENTATION_PATHS =
            {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The HS256 signing key.
     *
     * <p>When no secret is configured a random one is generated for this boot. That keeps the
     * application runnable with no compiled-in secret, at the cost of rejecting previously issued
     * tokens after a restart, which is logged loudly. A configured secret shorter than 32 bytes is
     * refused outright: HS256 with a short key is a forgeable key.
     */
    @Bean
    public SecretKey jwtSecretKey(SecurityProperties properties) {
        String configuredSecret = properties.jwtSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            byte[] generated = new byte[MINIMUM_SECRET_BYTES];
            new SecureRandom().nextBytes(generated);
            LOG.warn("app.security.jwt-secret is not set, so an ephemeral key was generated: tokens will not survive a "
                    + "restart. Set APP_JWT_SECRET to at least {} bytes for any shared environment.",
                    MINIMUM_SECRET_BYTES);
            return new SecretKeySpec(generated, JWT_ALGORITHM);
        }

        byte[] secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("app.security.jwt-secret must be at least " + MINIMUM_SECRET_BYTES
                    + " bytes for HS256 but was " + secretBytes.length);
        }
        return new SecretKeySpec(secretBytes, JWT_ALGORITHM);
    }

    /**
     * Decodes and validates inbound tokens, including the issuer so that a token minted for another
     * environment cannot be replayed here.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            ProblemResponseWriter problemWriter,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        RestAuthenticationEntryPoint authenticationEntryPoint = new RestAuthenticationEntryPoint(problemWriter);
        RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(problemWriter);

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
                                        + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/assets/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/v1/assets/**").hasAnyRole("ANALYST", "ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/api/v1/assets/**").hasAnyRole("ANALYST", "ADMIN")
                            .requestMatchers("/api/v1/impact-assessments/**").authenticated();
                    if (properties.apiDocsPublic()) {
                        authorize.requestMatchers(API_DOCUMENTATION_PATHS).permitAll();
                    } else {
                        authorize.requestMatchers(API_DOCUMENTATION_PATHS).hasRole("ADMIN");
                    }
                    authorize
                            .requestMatchers("/actuator/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * Maps the {@code roles} claim to {@code ROLE_}-prefixed authorities, and ignores the default
     * {@code scope} mapping so that only the claim this application mints carries authority.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * CORS limited to the exact origins in configuration. Credentials are disabled because the API
     * authenticates with an explicit bearer header rather than a cookie, so wildcard-origin risk is
     * removed rather than mitigated.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "X-Correlation-Id", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        return source;
    }
}
