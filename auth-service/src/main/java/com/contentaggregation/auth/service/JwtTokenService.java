package com.contentaggregation.auth.service;

import com.contentaggregation.auth.entity.User;
import io.jsonwebtoken.Claims;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service interface for JWT token operations.
 */
public interface JwtTokenService {

    /**
     * Generates an access token for a user.
     *
     * @param user the user
     * @return JWT access token
     */
    String generateAccessToken(User user);

    /**
     * Generates a refresh token for a user.
     *
     * @param user the user
     * @return JWT refresh token
     */
    String generateRefreshToken(User user);

    /**
     * Validates an access token.
     *
     * @param token JWT token
     * @return true if valid
     */
    boolean validateAccessToken(String token);

    /**
     * Validates a refresh token.
     *
     * @param token JWT token
     * @return true if valid
     */
    boolean validateRefreshToken(String token);

    /**
     * Extracts user ID from token.
     *
     * @param token JWT token
     * @return user UUID
     */
    UUID getUserIdFromToken(String token);

    /**
     * Extracts email from token.
     *
     * @param token JWT token
     * @return user email
     */
    String getEmailFromToken(String token);

    /**
     * Checks if token is expired.
     *
     * @param token JWT token
     * @return true if expired
     */
    boolean isTokenExpired(String token);

    /**
     * Gets token expiration time.
     *
     * @param token JWT token
     * @return expiration timestamp
     */
    LocalDateTime getExpirationFromToken(String token);

    /**
     * Extracts all claims from token.
     *
     * @param token JWT token
     * @return claims
     */
    Claims extractAllClaims(String token);

    /**
     * Gets access token expiration time from now.
     *
     * @return expiration timestamp
     */
    LocalDateTime getAccessTokenExpiration();
}
