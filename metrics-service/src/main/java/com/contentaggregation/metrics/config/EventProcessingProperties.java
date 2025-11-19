package com.contentaggregation.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.events")
public class EventProcessingProperties {

    private final Validation validation = new Validation();
    private final RateLimit rateLimit = new RateLimit();

    public Validation getValidation() {
        return validation;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class Validation {
        /**
         * Maximum allowed future skew for event timestamps.
         */
        private Duration maxFutureSkew = Duration.ofMinutes(2);

        /**
         * Maximum age allowed for events.
         */
        private Duration maxAge = Duration.ofDays(7);

        public Duration getMaxFutureSkew() {
            return maxFutureSkew;
        }

        public void setMaxFutureSkew(Duration maxFutureSkew) {
            this.maxFutureSkew = maxFutureSkew;
        }

        public Duration getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Duration maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class RateLimit {
        private int limit = 1000;
        private Duration window = Duration.ofMinutes(1);

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}

