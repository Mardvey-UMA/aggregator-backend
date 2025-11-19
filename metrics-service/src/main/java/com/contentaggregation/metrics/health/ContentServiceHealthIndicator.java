package com.contentaggregation.metrics.health;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ContentServiceHealthIndicator implements HealthIndicator {

    private final WebClient contentServiceWebClient;

    public ContentServiceHealthIndicator(@Qualifier("contentServiceWebClient") WebClient contentServiceWebClient) {
        this.contentServiceWebClient = contentServiceWebClient;
    }

    @Override
    public Health health() {
        try {
            contentServiceWebClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(ex -> Mono.error(new IllegalStateException("Content service unreachable", ex)))
                .block();
            return Health.up().build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}


