package com.contentaggregation.content.client.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO representing user data from auth service.
 */
public record UserDto(
        UUID id,
        String email,
        String username,
        boolean emailVerified,
        boolean enabled,
        Set<String> roles,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    /**
     * Checks if user has admin role.
     */
    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }
}
