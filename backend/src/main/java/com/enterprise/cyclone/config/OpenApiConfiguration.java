package com.enterprise.cyclone.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for Swagger UI and generated clients.
 *
 * <p>The bearer scheme is declared globally, so every operation shows as authenticated except the
 * token endpoint, which opts out with an empty {@code @SecurityRequirements}. The server list is
 * built from the configured CORS origins, which is why the Authorize button sends the token to
 * exactly the host the browser is already talking to.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI cycloneImpactOpenApi(
            @Value("${app.security.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        return new OpenAPI()
                .info(new Info()
                        .title("Cyclone Impact & Infrastructure Vulnerability Forecaster")
                        .version("1.0.0")
                        .description("""
                                Assesses critical infrastructure exposure against a tropical cyclone track.

                                Obtain a token from POST /api/v1/auth/token, click Authorize, and paste the \
                                accessToken value. Roles: VIEWER reads and assesses, ANALYST also writes the \
                                asset registry, ADMIN also reads operational endpoints.""")
                        .contact(new Contact().name("Cyclone Impact Platform"))
                        .license(new License().name("Internal use")))
                .servers(serverList(allowedOrigins))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("HS256 JWT issued by /api/v1/auth/token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    private static List<Server> serverList(List<String> allowedOrigins) {
        return List.of(new Server().url("/").description("This API"),
                new Server().url(allowedOrigins.get(0)).description("Frontend origin"));
    }
}
