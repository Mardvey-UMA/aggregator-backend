package com.contentaggregation.content.client;

import com.contentaggregation.content.client.dto.UserDto;
import com.contentaggregation.content.exception.AuthenticationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for communicating with the auth service.
 *
 * <p>Uses circuit breaker pattern for fault tolerance when auth service is unavailable.
 */
@Service
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final WebClient webClient;

    public AuthServiceClient(WebClient authServiceWebClient) {
        this.webClient = authServiceWebClient;
    }

    /**
     * Validates a JWT token by calling the auth service.
     *
     * @param token the JWT token (without "Bearer " prefix)
     * @return user information if token is valid
     * @throws AuthenticationException if token is invalid or service unavailable
     */
    @CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
    @Retry(name = "authService")
    public UserDto validateToken(String token) {
        log.debug("Validating token with auth service");

        try {
            return webClient.get()
                    .uri("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Token validation failed: Unauthorized");
            throw new AuthenticationException("Invalid or expired token");
        } catch (WebClientResponseException.Forbidden e) {
            log.warn("Token validation failed: Forbidden");
            throw new AuthenticationException("Access denied");
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            throw new AuthenticationException("Token validation failed", e);
        }
    }

    /**
     * Fallback method when auth service is unavailable.
     */
    @SuppressWarnings("unused")
    private UserDto validateTokenFallback(String token, Exception ex) {
        log.warn("Auth service unavailable, using fallback: {}", ex.getMessage());
        throw new AuthenticationException("Authentication service is temporarily unavailable. Please try again later.");
    }

    /**
     * Checks if auth service is healthy.
     *
     * @return true if auth service responds to health check
     */
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
