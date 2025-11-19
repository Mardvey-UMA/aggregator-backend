package com.contentaggregation.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata for paginated responses.
 */
@Schema(description = "Pagination metadata")
public record PageMetadata(
        @Schema(description = "Current page number (0-indexed)")
        int page,

        @Schema(description = "Page size")
        int size,

        @Schema(description = "Total number of elements")
        long totalElements,

        @Schema(description = "Total number of pages")
        int totalPages,

        @Schema(description = "Whether there is a next page")
        boolean hasNext,

        @Schema(description = "Whether there is a previous page")
        boolean hasPrevious
) {
    /**
     * Creates PageMetadata from a Spring Page.
     */
    public static PageMetadata from(Page<?> page) {
        return new PageMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
