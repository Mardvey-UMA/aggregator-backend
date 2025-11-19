package com.contentaggregation.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/**
 * Main application class for the Authentication Service.
 *
 * <p>This service handles user authentication and authorization including:
 * <ul>
 *   <li>Email/password registration and login</li>
 *   <li>VK OAuth2 authentication</li>
 *   <li>JWT token generation and validation</li>
 *   <li>Token refresh and revocation</li>
 * </ul>
 *
 * @author Content Aggregation Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    /**
     * Sets the default timezone to UTC for consistent timestamp handling.
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
