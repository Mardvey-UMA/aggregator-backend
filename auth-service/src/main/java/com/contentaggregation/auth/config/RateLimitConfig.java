package com.contentaggregation.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Configuration for rate limiting using Redis.
 *
 * <p>Provides a simple rate limiter that tracks request counts per key (IP or user).
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter(StringRedisTemplate redisTemplate) {
        return new RateLimiter(redisTemplate);
    }

    /**
     * Simple rate limiter using Redis for distributed rate limiting.
     */
    public static class RateLimiter {

        private final StringRedisTemplate redisTemplate;
        private final ConcurrentMap<String, Long> localCache = new ConcurrentHashMap<>();

        public RateLimiter(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * Checks if a request is allowed based on rate limit.
         *
         * @param key unique key (e.g., IP address or user ID)
         * @param limit maximum requests allowed
         * @param window time window for the limit
         * @return true if request is allowed
         */
        public boolean isAllowed(String key, int limit, Duration window) {
            try {
                String redisKey = "rate_limit:" + key;
                Long count = redisTemplate.opsForValue().increment(redisKey);

                if (count != null && count == 1) {
                    redisTemplate.expire(redisKey, window);
                }

                boolean allowed = count == null || count <= limit;

                if (!allowed) {
                    log.warn("Rate limit exceeded for key: {}", key);
                }

                return allowed;
            } catch (Exception ex) {
                // Fallback to local cache if Redis is unavailable
                log.warn("Redis unavailable for rate limiting, using local cache: {}", ex.getMessage());
                return isAllowedLocal(key, limit, window);
            }
        }

        /**
         * Local fallback rate limiter when Redis is unavailable.
         */
        private boolean isAllowedLocal(String key, int limit, Duration window) {
            long now = System.currentTimeMillis();
            long windowMs = window.toMillis();

            Long lastReset = localCache.get(key + ":reset");
            Long count = localCache.get(key + ":count");

            if (lastReset == null || now - lastReset > windowMs) {
                localCache.put(key + ":reset", now);
                localCache.put(key + ":count", 1L);
                return true;
            }

            long newCount = (count == null ? 0 : count) + 1;
            localCache.put(key + ":count", newCount);

            return newCount <= limit;
        }

        /**
         * Gets remaining requests for a key.
         *
         * @param key unique key
         * @param limit maximum requests
         * @return remaining requests
         */
        public int getRemaining(String key, int limit) {
            try {
                String redisKey = "rate_limit:" + key;
                String value = redisTemplate.opsForValue().get(redisKey);

                if (value == null) {
                    return limit;
                }

                int used = Integer.parseInt(value);
                return Math.max(0, limit - used);
            } catch (Exception ex) {
                return limit;
            }
        }
    }
}
