package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@JsonTypeName(BaseEvent.EventType.Names.SESSION)
@Schema(description = "Event emitted when a session starts or ends")
public record SessionEvent(
    BaseEvent base,

    @Valid
    @NotNull
    SessionData data
) implements UserEventPayload {

    public record SessionData(
        @Positive
        @Schema(description = "Session duration (ms)", example = "120000")
        Long durationMs,

        @PositiveOrZero
        @Schema(description = "Number of posts viewed in the session", example = "42")
        Integer postsViewed,

        @PositiveOrZero
        @Schema(description = "Number of interactions performed", example = "18")
        Integer interactions
    ) { }
}

