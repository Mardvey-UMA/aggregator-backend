package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonTypeName(BaseEvent.EventType.Names.POST_IMPRESSION)
@Schema(description = "Event emitted when a post impression occurs in the feed")
public record PostImpressionEvent(
    BaseEvent base,

    @Valid
    @NotNull
    ImpressionData data
) implements UserEventPayload {

    @Schema(description = "Payload specific to impression events")
    public record ImpressionData(
        @NotBlank @Size(max = 128)
        @Schema(description = "Identifier of the content item shown", example = "msg_123")
        String messageId,

        @NotNull @Min(0) @Max(200)
        @Schema(description = "Zero-based position of the post in the feed", example = "3")
        Integer positionInFeed,

        @Size(max = 64)
        @Schema(description = "Optional recommendation tracking identifier", example = "rec_abc")
        String recommendationId,

        @DecimalMin("0.0")
        @Schema(description = "Recommendation score if applicable", example = "0.87")
        Double recommendationScore,

        @NotBlank
        @Pattern(regexp = "recommended|trending|search|manual", message = "source must be recommended, trending, search, or manual")
        @Schema(description = "Source of the impression", example = "recommended")
        String source
    ) { }
}

