package com.contentaggregation.metrics.client.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

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
    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }
}

