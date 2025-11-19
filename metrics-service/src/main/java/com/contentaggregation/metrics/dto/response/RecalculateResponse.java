package com.contentaggregation.metrics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of profile recalculation results")
public record RecalculateResponse(
    @Schema(description = "Number of user profiles recalculated")
    int profiles
) {
}


