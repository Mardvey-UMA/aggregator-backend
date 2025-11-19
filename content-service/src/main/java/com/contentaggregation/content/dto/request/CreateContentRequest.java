package com.contentaggregation.content.dto.request;

import com.contentaggregation.content.enums.ContentType;
import com.contentaggregation.content.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating content manually.
 */
@Schema(description = "Request to create new content")
public record CreateContentRequest(
        @Schema(description = "Content title", example = "New Technology Breakthrough")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        @Schema(description = "Content text", example = "This is the main content...")
        @NotBlank(message = "Content is required")
        @Size(max = 4096, message = "Content must not exceed 4096 characters")
        String content,

        @Schema(description = "Content type")
        @NotNull(message = "Content type is required")
        ContentType contentType,

        @Schema(description = "External link URL")
        String linkUrl,

        @Schema(description = "Media URLs")
        List<String> mediaUrls,

        @Schema(description = "Media type")
        MediaType mediaType,

        @Schema(description = "Categories with scores", example = "{\"technology\": 0.9, \"science\": 0.5}")
        Map<String, Double> categories,

        @Schema(description = "Keywords")
        List<String> keywords,

        @Schema(description = "Source channel name")
        String sourceChannelName
) {}
