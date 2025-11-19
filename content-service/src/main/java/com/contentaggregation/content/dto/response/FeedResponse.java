package com.contentaggregation.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for content feed with pagination metadata.
 */
@Schema(description = "Paginated content feed response")
public record FeedResponse(
        @Schema(description = "List of content items")
        List<ContentDto> content,

        @Schema(description = "Pagination metadata")
        PageMetadata pagination
) {
    /**
     * Creates a FeedResponse from a Spring Page.
     */
    public static FeedResponse from(org.springframework.data.domain.Page<ContentDto> page) {
        return new FeedResponse(
                page.getContent(),
                PageMetadata.from(page)
        );
    }
}
