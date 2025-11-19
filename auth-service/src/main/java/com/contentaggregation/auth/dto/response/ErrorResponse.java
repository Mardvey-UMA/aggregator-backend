package com.contentaggregation.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response following RFC 7807.
 *
 * @param timestamp when the error occurred
 * @param status HTTP status code
 * @param error error type
 * @param message error message
 * @param path request path
 * @param errors validation errors (if applicable)
 */
@Schema(description = "Standard error response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Timestamp when error occurred", example = "2025-01-15T10:30:00")
    LocalDateTime timestamp,

    @Schema(description = "HTTP status code", example = "400")
    int status,

    @Schema(description = "Error type", example = "Bad Request")
    String error,

    @Schema(description = "Error message", example = "Validation failed")
    String message,

    @Schema(description = "Request path", example = "/api/v1/auth/register")
    String path,

    @Schema(description = "Validation errors")
    List<FieldError> errors
) {
    /**
     * Field-level validation error.
     */
    @Schema(description = "Field validation error")
    public record FieldError(
        @Schema(description = "Field name", example = "email")
        String field,

        @Schema(description = "Error message", example = "Email is required")
        String message
    ) {}

    /**
     * Creates an error response without field errors.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    /**
     * Creates an error response with field errors.
     */
    public static ErrorResponse of(int status, String error, String message, String path, List<FieldError> errors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, errors);
    }
}
