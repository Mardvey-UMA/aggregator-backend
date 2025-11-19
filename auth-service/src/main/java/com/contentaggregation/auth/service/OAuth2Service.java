package com.contentaggregation.auth.service;

import com.contentaggregation.auth.dto.response.AuthResponse;
import com.contentaggregation.auth.dto.response.VKUserInfo;
import com.contentaggregation.auth.entity.User;

/**
 * Service interface for OAuth2 authentication operations.
 */
public interface OAuth2Service {

    /**
     * Generates the VK authorization URL.
     *
     * @param redirectUri the callback redirect URI
     * @param state optional state parameter for CSRF protection
     * @return authorization URL
     */
    String getVKAuthorizationUrl(String redirectUri, String state);

    /**
     * Authenticates a user with VK OAuth2.
     *
     * @param code authorization code from VK
     * @param redirectUri callback redirect URI
     * @return authentication response with tokens
     */
    AuthResponse authenticateWithVK(String code, String redirectUri);

    /**
     * Gets or creates a user from VK user info.
     *
     * @param vkUserInfo VK user information
     * @param accessToken VK access token
     * @return user entity
     */
    User getOrCreateUserFromVK(VKUserInfo vkUserInfo, String accessToken);
}
