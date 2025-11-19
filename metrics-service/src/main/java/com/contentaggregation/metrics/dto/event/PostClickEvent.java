package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@JsonTypeName(BaseEvent.EventType.Names.POST_CLICK)
@Schema(description = "Event emitted when a user clicks into a post")
public record PostClickEvent(
    BaseEvent base,

    @Valid
    @NotNull
    PostClickData data
) implements UserEventPayload {

    public record PostClickData(
        @NotBlank @Size(max = 128)
        @Schema(description = "Identifier of the clicked post", example = "msg_123")
        String messageId,

        @NotNull @Min(0) @Max(200)
        @Schema(description = "Zero-based position within the feed", example = "2")
        Integer positionInFeed,

        @PositiveOrZero
        @Schema(description = "Delay from impression to click (ms)", example = "350")
        Integer timeSinceImpressionMs
    ) { }
}

