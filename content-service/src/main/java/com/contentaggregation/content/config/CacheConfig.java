package com.contentaggregation.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for content service.
 *
 * <p>Configures caching with different TTLs for different cache types:
 * <ul>
 *   <li>feed: User personalized feeds (5 minutes)</li>
 *   <li>coldStartFeed: Default feed for new users (5 minutes)</li>
 *   <li>trending: Trending content (15 minutes)</li>
 *   <li>content: Individual content items (60 minutes)</li>
 *   <li>userPreferences: User preferences (30 minutes)</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.content-ttl-minutes:60}")
    private long contentTtlMinutes;

    @Value("${app.cache.feed-ttl-minutes:5}")
    private long feedTtlMinutes;

    @Value("${app.cache.trending-ttl-minutes:15}")
    private long trendingTtlMinutes;

    @Value("${app.cache.user-preferences-ttl-minutes:30}")
    private long userPreferencesTtlMinutes;

    @Value("${app.cache.auth-token-ttl-seconds:60}")
    private long authTokenTtlSeconds;

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(contentTtlMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisJsonSerializer))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Feed cache (5 minutes)
        cacheConfigurations.put("feed", defaultConfig
                .entryTtl(Duration.ofMinutes(feedTtlMinutes))
                .prefixCacheNameWith("content-service:"));

        // Cold start feed cache (5 minutes)
        cacheConfigurations.put("coldStartFeed", defaultConfig
                .entryTtl(Duration.ofMinutes(feedTtlMinutes))
                .prefixCacheNameWith("content-service:"));

        // Trending cache (15 minutes)
        cacheConfigurations.put("trending", defaultConfig
                .entryTtl(Duration.ofMinutes(trendingTtlMinutes))
                .prefixCacheNameWith("content-service:"));

        // Content cache (60 minutes)
        cacheConfigurations.put("content", defaultConfig
                .entryTtl(Duration.ofMinutes(contentTtlMinutes))
                .prefixCacheNameWith("content-service:"));

        // User preferences cache (30 minutes)
        cacheConfigurations.put("userPreferences", defaultConfig
                .entryTtl(Duration.ofMinutes(userPreferencesTtlMinutes))
                .prefixCacheNameWith("content-service:"));

        // Similar content cache (30 minutes)
        cacheConfigurations.put("similarContent", defaultConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("content-service:"));

        // Auth token cache (seconds)
        cacheConfigurations.put("authTokens", defaultConfig
                .entryTtl(Duration.ofSeconds(authTokenTtlSeconds))
                .prefixCacheNameWith("content-service:"));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.prefixCacheNameWith("content-service:"))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
