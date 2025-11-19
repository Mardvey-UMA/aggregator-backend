package com.contentaggregation.metrics.service.ingestion;

import com.contentaggregation.metrics.config.RedisConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class EventDeduplicationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisConfig.DeduplicationProperties properties;

    public EventDeduplicationService(
        RedisTemplate<String, String> redisTemplate,
        RedisConfig.DeduplicationProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean isDuplicateEvent(String eventId) {
        return isDuplicate("event:dedup:" + eventId);
    }

    public boolean isDuplicateBatch(String batchId) {
        return isDuplicate("batch:dedup:" + batchId);
    }

    private boolean isDuplicate(String key) {
        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", properties.ttlSeconds(), TimeUnit.SECONDS);
        return !Boolean.TRUE.equals(isNew);
    }
}

