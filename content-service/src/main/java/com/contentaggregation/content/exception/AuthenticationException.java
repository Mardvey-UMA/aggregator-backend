package com.contentaggregation.content.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends RuntimeException {

    private final String code;

    public AuthenticationException(String message) {
        super(message);
        this.code = "AUTHENTICATION_FAILED";
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.code = "AUTHENTICATION_FAILED";
    }

    public String getCode() {
        return code;
    }
}
