package com.contentaggregation.content.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Request DTO for customizing feed content.
 */
@Schema(description = "Request parameters for personalized feed")
public record FeedRequest(
        @Schema(description = "Categories to include")
        List<String> categories,

        @Schema(description = "Content types to include")
        List<String> contentTypes,

        @Schema(description = "Preferred language")
        String language,

        @Schema(description = "Exclude content IDs")
        List<String> excludeIds
) {}
