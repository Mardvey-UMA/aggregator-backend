package com.contentaggregation.metrics.service.ingestion;

import com.contentaggregation.metrics.config.EventProcessingProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EventProcessingProperties properties;

    public RateLimiterService(
        RedisTemplate<String, String> redisTemplate,
        EventProcessingProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void assertWithinLimit(UUID userId, int events) {
        EventProcessingProperties.RateLimit rateLimit = properties.getRateLimit();
        String key = "ratelimit:events:" + userId;
        Long currentCount = redisTemplate.opsForValue().increment(key, events);

        if (currentCount != null && currentCount.equals((long) events)) {
            redisTemplate.expire(key, rateLimit.getWindow());
        }

        if (currentCount != null && currentCount > rateLimit.getLimit()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Event ingestion rate limit exceeded");
        }
    }
}

