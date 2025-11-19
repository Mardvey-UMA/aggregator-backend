package com.contentaggregation.metrics.health;

import com.contentaggregation.metrics.monitoring.ProfileProcessingLagMonitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ProfileProcessingHealthIndicator implements HealthIndicator {

    private final ProfileProcessingLagMonitor lagMonitor;
    private final long maxLagMs;

    public ProfileProcessingHealthIndicator(
        ProfileProcessingLagMonitor lagMonitor,
        @Value("${app.health.profile-processing.max-lag-ms:1000}") long maxLagMs
    ) {
        this.lagMonitor = lagMonitor;
        this.maxLagMs = maxLagMs;
    }

    @Override
    public Health health() {
        long currentLag = lagMonitor.currentLag();
        if (currentLag <= maxLagMs) {
            return Health.up()
                .withDetail("lagMs", currentLag)
                .withDetail("thresholdMs", maxLagMs)
                .build();
        }

        return Health.down()
            .withDetail("lagMs", currentLag)
            .withDetail("thresholdMs", maxLagMs)
            .build();
    }
}


