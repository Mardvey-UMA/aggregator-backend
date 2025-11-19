package com.contentaggregation.metrics.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Payload for manually updating user preference weights")
public record UpdatePreferencesRequest(
    @NotNull
    @NotEmpty
    @Schema(
        description = "Category weights normalized between 0 and 1",
        example = """
            {
              "technology": 0.85,
              "finance": 0.55,
              "sports": 0.15
            }
            """
    )
    Map<String, Double> categories
) {
}


