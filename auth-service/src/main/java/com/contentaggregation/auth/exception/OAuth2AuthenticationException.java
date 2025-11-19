package com.contentaggregation.auth.exception;

/**
 * Exception thrown when OAuth2 authentication fails.
 */
public class OAuth2AuthenticationException extends BusinessException {

    public OAuth2AuthenticationException(String message) {
        super("OAUTH2_AUTH_FAILED", message);
    }

    public OAuth2AuthenticationException(String message, Throwable cause) {
        super("OAUTH2_AUTH_FAILED", message, cause);
    }

    public static OAuth2AuthenticationException tokenExchangeFailed() {
        return new OAuth2AuthenticationException("Failed to exchange authorization code for access token");
    }

    public static OAuth2AuthenticationException userInfoFailed() {
        return new OAuth2AuthenticationException("Failed to fetch user information from OAuth2 provider");
    }

    public static OAuth2AuthenticationException invalidState() {
        return new OAuth2AuthenticationException("Invalid OAuth2 state parameter");
    }

    public static OAuth2AuthenticationException emailRequired() {
        return new OAuth2AuthenticationException("Email is required for OAuth2 registration");
    }
}
