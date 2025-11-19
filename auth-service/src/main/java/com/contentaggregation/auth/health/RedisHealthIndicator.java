package com.contentaggregation.auth.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Redis connectivity.
 */
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "Connected")
                        .withDetail("response", pong)
                        .build();
            }
            return Health.down()
                    .withDetail("redis", "Unexpected response")
                    .withDetail("response", pong)
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("redis", "Connection failed")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
