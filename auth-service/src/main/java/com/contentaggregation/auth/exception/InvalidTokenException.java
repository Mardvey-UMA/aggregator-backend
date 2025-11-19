package com.contentaggregation.auth.exception;

/**
 * Exception thrown when a token is invalid, expired, or revoked.
 */
public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message);
    }

    public static InvalidTokenException expired() {
        return new InvalidTokenException("Token has expired");
    }

    public static InvalidTokenException revoked() {
        return new InvalidTokenException("Token has been revoked");
    }

    public static InvalidTokenException malformed() {
        return new InvalidTokenException("Token is malformed or invalid");
    }

    public static InvalidTokenException notFound() {
        return new InvalidTokenException("Token not found");
    }
}
