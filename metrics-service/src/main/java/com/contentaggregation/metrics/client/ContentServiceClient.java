package com.contentaggregation.metrics.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ContentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ContentServiceClient.class);
    private final WebClient webClient;

    public ContentServiceClient(@Qualifier("contentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "contentService", fallbackMethod = "metadataFallback")
    @Retry(name = "contentService")
    public ContentMetadata getContentMetadata(String messageId) {
        return webClient.get()
            .uri("/api/v1/content/{id}/metadata", messageId)
            .retrieve()
            .bodyToMono(ContentMetadata.class)
            .timeout(Duration.ofSeconds(3))
            .onErrorResume(throwable -> {
                log.warn("Content metadata fetch failed for {}: {}", messageId, throwable.getMessage());
                return Mono.empty();
            })
            .blockOptional()
            .orElse(ContentMetadata.empty());
    }

    @SuppressWarnings("unused")
    private ContentMetadata metadataFallback(String messageId, Throwable throwable) {
        log.warn("Content service fallback for {} due to {}", messageId, throwable.getMessage());
        return ContentMetadata.empty();
    }

    public record ContentMetadata(
        Map<String, Double> categories,
        Map<String, List<String>> entities,
        String sentiment,
        boolean isClickbait,
        String contentType
    ) {
        public static ContentMetadata empty() {
            return new ContentMetadata(Collections.emptyMap(), Collections.emptyMap(), "neutral", false, "unknown");
        }
    }
}

