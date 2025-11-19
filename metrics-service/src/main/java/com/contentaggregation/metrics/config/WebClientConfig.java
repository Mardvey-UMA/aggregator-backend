package com.contentaggregation.metrics.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
@EnableConfigurationProperties({
    WebClientConfig.ContentServiceProperties.class,
    WebClientConfig.AuthServiceProperties.class
})
public class WebClientConfig {

    @Bean
    public WebClient contentServiceWebClient(WebClient.Builder builder, ContentServiceProperties properties) {
        return buildWebClient(builder, properties, "content-service-connector");
    }

    @Bean
    public WebClient authServiceWebClient(WebClient.Builder builder, AuthServiceProperties properties) {
        return buildWebClient(builder, properties, "auth-service-connector");
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer(
        ContentServiceProperties contentServiceProperties,
        AuthServiceProperties authServiceProperties
    ) {
        return factory -> {
            configureCircuitBreaker(factory, contentServiceProperties);
            configureCircuitBreaker(factory, authServiceProperties);
        };
    }

    private void configureCircuitBreaker(
        ReactiveResilience4JCircuitBreakerFactory factory,
        BaseServiceProperties properties
    ) {
        factory.configure(builder ->
                builder.circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(properties.circuitBreakerWindowSize())
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(properties.timeout())
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .recordExceptions(java.io.IOException.class, TimeoutException.class)
                        .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(properties.timeout())
                        .cancelRunningFuture(true)
                        .build()),
            properties.circuitBreakerId());
    }

    private WebClient buildWebClient(WebClient.Builder builder, BaseServiceProperties properties, String connectorName) {
        ConnectionProvider provider = ConnectionProvider.builder(connectorName)
            .maxConnections(properties.maxConnections())
            .pendingAcquireTimeout(properties.timeout())
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .responseTimeout(properties.timeout())
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis());

        return builder
            .baseUrl(properties.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(properties.maxInMemorySize()))
                .build())
            .build();
    }

    interface BaseServiceProperties {
        String baseUrl();
        Duration timeout();
        Duration connectTimeout();
        int maxConnections();
        int maxInMemorySize();
        int circuitBreakerWindowSize();
        String circuitBreakerId();
    }

    @ConfigurationProperties(prefix = "app.content-service")
    public static class ContentServiceProperties implements BaseServiceProperties {
        private String baseUrl = "http://localhost:8081";
        private Duration timeout = Duration.ofSeconds(3);
        private Duration connectTimeout = Duration.ofSeconds(2);
        private int maxConnections = 200;
        private int maxInMemorySize = 4 * 1024 * 1024;
        private int circuitBreakerWindowSize = 20;
        private String circuitBreakerId = "contentService";

        public String baseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration timeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration connectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int maxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int maxInMemorySize() {
            return maxInMemorySize;
        }

        public void setMaxInMemorySize(int maxInMemorySize) {
            this.maxInMemorySize = maxInMemorySize;
        }

        public int circuitBreakerWindowSize() {
            return circuitBreakerWindowSize;
        }

        public void setCircuitBreakerWindowSize(int circuitBreakerWindowSize) {
            this.circuitBreakerWindowSize = circuitBreakerWindowSize;
        }

        public String circuitBreakerId() {
            return circuitBreakerId;
        }

        public void setCircuitBreakerId(String circuitBreakerId) {
            this.circuitBreakerId = circuitBreakerId;
        }
    }

    @ConfigurationProperties(prefix = "app.auth-service")
    public static class AuthServiceProperties implements BaseServiceProperties {
        private String baseUrl = "http://localhost:8080";
        private Duration timeout = Duration.ofSeconds(3);
        private Duration connectTimeout = Duration.ofSeconds(2);
        private int maxConnections = 200;
        private int maxInMemorySize = 2 * 1024 * 1024;
        private int circuitBreakerWindowSize = 20;
        private String circuitBreakerId = "authService";

        public String baseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration timeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration connectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int maxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int maxInMemorySize() {
            return maxInMemorySize;
        }

        public void setMaxInMemorySize(int maxInMemorySize) {
            this.maxInMemorySize = maxInMemorySize;
        }

        public int circuitBreakerWindowSize() {
            return circuitBreakerWindowSize;
        }

        public void setCircuitBreakerWindowSize(int circuitBreakerWindowSize) {
            this.circuitBreakerWindowSize = circuitBreakerWindowSize;
        }

        public String circuitBreakerId() {
            return circuitBreakerId;
        }

        public void setCircuitBreakerId(String circuitBreakerId) {
            this.circuitBreakerId = circuitBreakerId;
        }
    }
}

