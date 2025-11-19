package com.contentaggregation.content.health;

import com.contentaggregation.content.repository.ContentPostRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for minimum content threshold.
 */
@Component
public class ContentAvailabilityHealthIndicator implements HealthIndicator {

    private static final long MINIMUM_CONTENT_THRESHOLD = 10;

    private final ContentPostRepository contentPostRepository;

    public ContentAvailabilityHealthIndicator(ContentPostRepository contentPostRepository) {
        this.contentPostRepository = contentPostRepository;
    }

    @Override
    public Health health() {
        try {
            long contentCount = contentPostRepository.count();

            if (contentCount >= MINIMUM_CONTENT_THRESHOLD) {
                return Health.up()
                        .withDetail("contentCount", contentCount)
                        .withDetail("minimumThreshold", MINIMUM_CONTENT_THRESHOLD)
                        .build();
            } else {
                return Health.down()
                        .withDetail("contentCount", contentCount)
                        .withDetail("minimumThreshold", MINIMUM_CONTENT_THRESHOLD)
                        .withDetail("error", "Content count below minimum threshold")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
