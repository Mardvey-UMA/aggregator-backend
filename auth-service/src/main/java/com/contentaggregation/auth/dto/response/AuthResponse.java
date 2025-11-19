package com.contentaggregation.auth.dto.response;

import java.time.LocalDateTime;

/**
 * Response DTO for authentication operations.
 *
 * @param accessToken JWT access token
 * @param refreshToken JWT refresh token
 * @param tokenType token type (always "Bearer")
 * @param expiresAt access token expiration timestamp
 * @param user authenticated user information
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    LocalDateTime expiresAt,
    UserDto user
) {
    /**
     * Creates an AuthResponse with Bearer token type.
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param expiresAt access token expiration
     * @param user user information
     * @return AuthResponse
     */
    public static AuthResponse of(String accessToken, String refreshToken,
                                   LocalDateTime expiresAt, UserDto user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresAt, user);
    }
}
