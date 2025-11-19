package com.contentaggregation.metrics.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProfileProcessingLagMonitor {

    private final AtomicLong lagMillis = new AtomicLong(0L);

    public ProfileProcessingLagMonitor(MeterRegistry meterRegistry) {
        Gauge.builder("events.processing.lag", lagMillis, AtomicLong::get)
            .description("Latest Kafka to database processing lag in milliseconds")
            .register(meterRegistry);
    }

    public void recordLag(long value) {
        lagMillis.set(Math.max(0, value));
    }

    public long currentLag() {
        return lagMillis.get();
    }
}


