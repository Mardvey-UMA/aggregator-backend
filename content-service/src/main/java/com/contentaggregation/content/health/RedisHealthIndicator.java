package com.contentaggregation.content.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Redis connectivity.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return Health.down()
                        .withDetail("error", "No connection factory available")
                        .build();
            }

            long startTime = System.currentTimeMillis();
            String pong = connectionFactory.getConnection().ping();
            long duration = System.currentTimeMillis() - startTime;

            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "Connected")
                        .withDetail("responseTimeMs", duration)
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Unexpected ping response: " + pong)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
