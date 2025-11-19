package com.contentaggregation.auth.service;

import com.contentaggregation.auth.dto.request.LoginRequest;
import com.contentaggregation.auth.dto.request.RegisterRequest;
import com.contentaggregation.auth.dto.response.AuthResponse;

/**
 * Service interface for authentication operations.
 */
public interface AuthenticationService {

    /**
     * Registers a new user with email and password.
     *
     * @param request registration details
     * @return authentication response with tokens
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with email and password.
     *
     * @param request login credentials
     * @return authentication response with tokens
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return authentication response with new tokens
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Logs out a user by revoking their refresh token.
     *
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);

    /**
     * Verifies a user's email using a verification token.
     *
     * @param token email verification token
     */
    void verifyEmail(String token);
}
