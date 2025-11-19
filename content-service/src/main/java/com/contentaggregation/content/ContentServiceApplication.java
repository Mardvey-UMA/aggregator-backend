package com.contentaggregation.content;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * Main application class for the Content Service.
 *
 * <p>The Content Service provides content aggregation, recommendation, and delivery
 * functionality for the platform. It supports multiple content types and sources,
 * with caching and enrichment capabilities.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }

    /**
     * Configures the application timezone to UTC on startup.
     *
     * <p>This ensures consistent timestamp handling across all components
     * and database operations.
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
