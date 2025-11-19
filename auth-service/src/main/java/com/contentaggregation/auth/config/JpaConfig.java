package com.contentaggregation.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * JPA and database configuration for the authentication service.
 *
 * <p>Configures:
 * <ul>
 *   <li>JPA auditing for automatic timestamp fields</li>
 *   <li>Repository scanning</li>
 *   <li>Transaction management</li>
 *   <li>UTC timezone for all date operations</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
@EnableJpaRepositories(basePackages = {
        "com.contentaggregation.auth.repository",
        "com.contentaggregation.auth.onboarding.repository"
})
@EnableTransactionManagement
public class JpaConfig {

    /**
     * Provides UTC datetime for JPA auditing.
     *
     * <p>This ensures all @CreatedDate and @LastModifiedDate annotations
     * use UTC timezone for consistency.
     *
     * @return DateTimeProvider that returns current UTC time
     */
    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneOffset.UTC));
    }
}
