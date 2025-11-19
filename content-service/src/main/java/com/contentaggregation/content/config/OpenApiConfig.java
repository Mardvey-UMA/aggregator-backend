package com.contentaggregation.content.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for Swagger documentation.
 *
 * <p>Configures API documentation with security schemes and metadata.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT authentication token obtained from auth service"
)
public class OpenApiConfig {

    @Value("${spring.application.name:content-service}")
    private String applicationName;

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Content Aggregation Service API")
                        .version("1.0.0")
                        .description("""
                                Content recommendation and aggregation service API.

                                ## Features
                                - Personalized content feeds based on user preferences
                                - Trending content discovery
                                - Content bookmarking and view tracking
                                - Similar content recommendations
                                - Category-based browsing
                                - Full-text search

                                ## Authentication
                                Most endpoints require JWT authentication. Obtain a token from the auth service
                                and include it in the Authorization header as: `Bearer <token>`

                                ## Rate Limiting
                                API calls are rate-limited. Contact support for increased limits.
                                """)
                        .contact(new Contact()
                                .name("Content Team")
                                .email("content@contentaggregation.com")
                                .url("https://github.com/contentaggregation"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.contentaggregation.com")
                                .description("Production server")
                ));
    }
}
