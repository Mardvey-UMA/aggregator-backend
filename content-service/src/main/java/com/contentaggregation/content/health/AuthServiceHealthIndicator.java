package com.contentaggregation.content.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Health indicator for auth service availability.
 */
@Component
public class AuthServiceHealthIndicator implements HealthIndicator {

    private final WebClient webClient;
    private final String authServiceUrl;

    public AuthServiceHealthIndicator(
            WebClient.Builder webClientBuilder,
            @Value("${app.auth-service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.authServiceUrl = authServiceUrl;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            String response = webClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            long duration = System.currentTimeMillis() - startTime;

            return Health.up()
                    .withDetail("authService", authServiceUrl)
                    .withDetail("responseTimeMs", duration)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("authService", authServiceUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
