package com.contentaggregation.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access token.
 *
 * @param refreshToken the refresh token
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
