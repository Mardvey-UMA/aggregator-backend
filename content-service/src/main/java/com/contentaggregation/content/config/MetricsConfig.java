package com.contentaggregation.content.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.DistributionSummary;
import com.contentaggregation.content.repository.ContentPostRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for custom Micrometer metrics.
 */
@Configuration
public class MetricsConfig {

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong diversityScore = new AtomicLong(70);

    @Bean
    public Counter feedRequestsCounter(MeterRegistry registry) {
        return Counter.builder("content.feed.requests")
                .description("Total number of feed requests")
                .tag("type", "personalized")
                .register(registry);
    }

    @Bean
    public Timer feedLatencyTimer(MeterRegistry registry) {
        return Timer.builder("content.feed.latency")
                .description("Feed request latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public Gauge cacheHitRatioGauge(MeterRegistry registry) {
        return Gauge.builder("content.cache.hit_ratio", this, config -> {
            long hits = config.cacheHits.get();
            long misses = config.cacheMisses.get();
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        })
                .description("Cache hit ratio")
                .register(registry);
    }

    @Bean
    public Gauge diversityScoreGauge(MeterRegistry registry) {
        return Gauge.builder("content.diversity.score", this, config ->
                config.diversityScore.get() / 100.0)
                .description("Feed diversity score (0-1)")
                .register(registry);
    }

    @Bean
    public DistributionSummary recommendationScoreSummary(MeterRegistry registry) {
        return DistributionSummary.builder("content.recommendation.score")
                .description("Distribution of recommendation scores")
                .publishPercentiles(0.5, 0.75, 0.95)
                .register(registry);
    }

    @Bean
    public Gauge totalContentGauge(MeterRegistry registry, ContentPostRepository repository) {
        return Gauge.builder("content.total.count", repository, ContentPostRepository::count)
                .description("Total content items in database")
                .register(registry);
    }

    @Bean
    public Counter bookmarkCounter(MeterRegistry registry) {
        return Counter.builder("content.bookmarks.total")
                .description("Total bookmarks created")
                .register(registry);
    }

    @Bean
    public Counter viewCounter(MeterRegistry registry) {
        return Counter.builder("content.views.total")
                .description("Total content views")
                .register(registry);
    }

    @Bean
    public Counter searchCounter(MeterRegistry registry) {
        return Counter.builder("content.search.requests")
                .description("Total search requests")
                .register(registry);
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void updateDiversityScore(double score) {
        diversityScore.set((long) (score * 100));
    }
}
