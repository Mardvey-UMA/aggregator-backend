package com.contentaggregation.auth.onboarding.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Custom Micrometer metrics for tracking onboarding engagement.
 */
@Component
public class OnboardingMetrics {

    private final MeterRegistry registry;
    private final Counter onboardingStarted;
    private final Counter onboardingCompleted;
    private final Counter onboardingSkipped;
    private final Timer onboardingDuration;

    public OnboardingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.onboardingStarted = Counter.builder("onboarding.started")
                .description("Number of users who started onboarding")
                .register(registry);

        this.onboardingCompleted = Counter.builder("onboarding.completed")
                .description("Number of users who completed onboarding")
                .register(registry);

        this.onboardingSkipped = Counter.builder("onboarding.skipped")
                .description("Number of users who skipped onboarding")
                .register(registry);

        this.onboardingDuration = Timer.builder("onboarding.duration")
                .description("Time taken to complete onboarding")
                .register(registry);
    }

    public void recordOnboardingStarted() {
        onboardingStarted.increment();
    }

    public void recordOnboardingCompleted(Duration duration) {
        onboardingCompleted.increment();
        onboardingDuration.record(duration);
    }

    public void recordOnboardingSkipped() {
        onboardingSkipped.increment();
    }

    public void recordCategorySelected(String category) {
        String tagValue = Objects.requireNonNullElse(category, "unknown");
        Counter.builder("onboarding.category.selected")
                .description("Category selection count")
                .tag("category", tagValue)
                .register(registry)
                .increment();
    }
}

