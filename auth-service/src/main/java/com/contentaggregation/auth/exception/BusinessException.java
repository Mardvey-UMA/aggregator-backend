package com.contentaggregation.auth.exception;

import lombok.Getter;

/**
 * Base exception for all business logic errors.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final String code;

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
