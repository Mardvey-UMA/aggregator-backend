package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@JsonTypeName(BaseEvent.EventType.Names.POST_DWELL)
@Schema(description = "Event emitted after a user finishes reading a post")
public record PostDwellEvent(
    BaseEvent base,

    @Valid
    @NotNull
    PostDwellData data
) implements UserEventPayload {

    public record PostDwellData(
        @NotBlank @Size(max = 128)
        @Schema(description = "Identifier of the post", example = "msg_123")
        String messageId,

        @Positive
        @Schema(description = "Total dwell time (ms)", example = "6400")
        Integer dwellTimeMs,

        @NotNull @DecimalMin("0.0")
        @Schema(description = "Scroll depth as a fraction (0-1)", example = "0.92")
        Double scrollDepth,

        @NotNull @PositiveOrZero
        @Schema(description = "Number of interactions during the session", example = "3")
        Integer interactions
    ) { }
}

