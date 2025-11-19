package com.contentaggregation.auth.exception;

/**
 * Exception thrown when login credentials are invalid.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message);
    }
}
