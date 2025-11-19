package com.contentaggregation.auth.service;

import com.contentaggregation.auth.dto.request.UpdateUserRequest;
import com.contentaggregation.auth.dto.response.UserDto;

import java.util.UUID;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Gets a user by ID.
     *
     * @param userId user ID
     * @return user DTO
     */
    UserDto getUserById(UUID userId);

    /**
     * Updates a user's profile.
     *
     * @param userId user ID
     * @param request update request
     * @return updated user DTO
     */
    UserDto updateUser(UUID userId, UpdateUserRequest request);

    /**
     * Deletes a user account.
     *
     * @param userId user ID
     */
    void deleteUser(UUID userId);
}
