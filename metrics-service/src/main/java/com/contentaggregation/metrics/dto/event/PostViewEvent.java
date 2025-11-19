package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@JsonTypeName(BaseEvent.EventType.Names.POST_VIEW)
@Schema(description = "Event emitted when a user keeps a post in view for a meaningful duration")
public record PostViewEvent(
    BaseEvent base,

    @Valid
    @NotNull
    PostViewData data
) implements UserEventPayload {

    public record PostViewData(
        @NotBlank @Size(max = 128)
        @Schema(description = "Identifier of the viewed post", example = "msg_123")
        String messageId,

        @NotNull @Min(0) @Max(200)
        @Schema(description = "Zero-based position of the post in the feed", example = "5")
        Integer positionInFeed,

        @NotNull @PositiveOrZero
        @Schema(description = "How long the post remained visible (ms)", example = "2400")
        Integer visibleDurationMs,

        @NotNull @DecimalMin("0.0")
        @Schema(description = "Portion of the viewport occupied (0-1)", example = "0.8")
        Double viewportPercentage
    ) { }
}

