package com.contentaggregation.auth.onboarding.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MetricsServiceClient {

    private final WebClient webClient;
    private final Duration timeout;
    private final String onboardingVersion;

    public MetricsServiceClient(
            WebClient.Builder builder,
            @Value("${metrics.service.url:http://localhost:8082}") String metricsUrl,
            @Value("${metrics.service.timeout-ms:5000}") long timeoutMs,
            @Value("${onboarding.version:v1}") String onboardingVersion
    ) {
        this.webClient = builder.baseUrl(metricsUrl).build();
        this.timeout = Duration.ofMillis(timeoutMs);
        this.onboardingVersion = onboardingVersion;
    }

    @CircuitBreaker(name = "metricsService", fallbackMethod = "initializeProfileFallback")
    public void initializeUserProfile(
            UUID userId,
            List<String> selectedCategories,
            List<String> selectedContentTypes
    ) {
        ProfileInitRequest request = new ProfileInitRequest(
                selectedCategories,
                selectedContentTypes,
                onboardingVersion,
                System.currentTimeMillis()
        );

        webClient.post()
                .uri("/api/v1/profiles/{userId}/initialize", userId)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .timeout(timeout)
                .block();
    }

    @SuppressWarnings("unused")
    private void initializeProfileFallback(
            UUID userId,
            List<String> selectedCategories,
            List<String> selectedContentTypes,
            Throwable ex
    ) {
        log.error("Failed to initialize profile for user {}, will retry later", userId, ex);
    }

    public record ProfileInitRequest(
            List<String> selectedCategories,
            List<String> selectedContentTypes,
            String onboardingVersion,
            long timestamp
    ) {
    }
}

