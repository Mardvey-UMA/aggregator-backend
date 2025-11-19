package com.contentaggregation.auth.config;

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
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT authentication. Provide the token without 'Bearer' prefix."
)
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Content Aggregation - Auth Service API")
                        .version("1.0.0")
                        .description("""
                                Authentication and Authorization API for Content Aggregation Platform.

                                ## Features
                                - User registration with email/password
                                - User login with JWT tokens
                                - OAuth2 authentication (VK)
                                - Token refresh
                                - User profile management
                                - Onboarding preference capture

                                ## Authentication
                                Protected endpoints require a JWT token in the Authorization header:
                                `Authorization: Bearer <token>`
                                
                                ## Onboarding API
                                Use the `/api/v1/onboarding/*` endpoints to collect initial user interests.
                                """)
                        .contact(new Contact()
                                .name("Content Aggregation Team")
                                .email("support@contentaggregation.com")
                                .url("https://github.com/contentaggregation"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api-staging.contentaggregation.com")
                                .description("Staging server"),
                        new Server()
                                .url("https://api.contentaggregation.com")
                                .description("Production server")
                ));
    }
}
