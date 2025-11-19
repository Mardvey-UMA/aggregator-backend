package com.contentaggregation.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user profile.
 *
 * @param username new username (optional)
 * @param email new email (optional)
 */
@Schema(description = "Update user profile request")
public record UpdateUserRequest(
    @Schema(description = "New username", example = "newusername")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @Schema(description = "New email address", example = "newemail@example.com")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email
) {}
