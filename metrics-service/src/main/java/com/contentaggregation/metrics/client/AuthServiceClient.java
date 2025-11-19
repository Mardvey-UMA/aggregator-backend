package com.contentaggregation.metrics.client;

import com.contentaggregation.metrics.client.dto.UserDto;
import com.contentaggregation.metrics.exception.AuthenticationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    private final WebClient webClient;

    public AuthServiceClient(@Qualifier("authServiceWebClient") WebClient authServiceWebClient) {
        this.webClient = authServiceWebClient;
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
    @Retry(name = "authService")
    public UserDto validateToken(String token) {
        try {
            return webClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(UserDto.class)
                .timeout(Duration.ofSeconds(5))
                .block();
        } catch (WebClientResponseException.Unauthorized e) {
            throw new AuthenticationException("Invalid or expired token");
        } catch (WebClientResponseException.Forbidden e) {
            throw new AuthenticationException("Access denied for token");
        } catch (Exception e) {
            throw new AuthenticationException("Token validation failed", e);
        }
    }

    @SuppressWarnings("unused")
    private UserDto validateTokenFallback(String token, Exception ex) {
        log.warn("Auth service fallback due to {}", ex.getMessage());
        throw new AuthenticationException("Authentication service is temporarily unavailable. Please try again later.");
    }

    public boolean isHealthy() {
        try {
            webClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(2))
                .block();
            return true;
        } catch (Exception e) {
            log.warn("Auth service health check failed: {}", e.getMessage());
            return false;
        }
    }
}

