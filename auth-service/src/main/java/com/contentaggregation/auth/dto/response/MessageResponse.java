package com.contentaggregation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simple message response.
 *
 * @param message the response message
 */
@Schema(description = "Simple message response")
public record MessageResponse(
    @Schema(description = "Response message", example = "Operation completed successfully")
    String message
) {}
