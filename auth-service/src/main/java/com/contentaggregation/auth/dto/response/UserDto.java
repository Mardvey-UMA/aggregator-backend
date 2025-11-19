package com.contentaggregation.auth.dto.response;

import com.contentaggregation.auth.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for user information.
 *
 * @param id user's unique identifier
 * @param email user's email address
 * @param username user's username
 * @param emailVerified whether email is verified
 * @param roles user's roles
 * @param createdAt account creation timestamp
 */
public record UserDto(
    UUID id,
    String email,
    String username,
    boolean emailVerified,
    Set<String> roles,
    LocalDateTime createdAt
) {
    /**
     * Creates a UserDto from a User entity.
     *
     * @param user the user entity
     * @return UserDto representation
     */
    public static UserDto fromEntity(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getEmailVerified(),
            user.getRolesAsSet(),
            user.getCreatedAt()
        );
    }
}
