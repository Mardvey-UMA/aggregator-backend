package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@JsonTypeName(BaseEvent.EventType.Names.POST_REACTION)
@Schema(description = "Event emitted when a user reacts to a post (like, dislike, bookmark, share)")
public record PostReactionEvent(
    BaseEvent base,

    @Valid
    @NotNull
    PostReactionData data
) implements UserEventPayload {

    public record PostReactionData(
        @NotBlank @Size(max = 128)
        @Schema(description = "Identifier of the post", example = "msg_123")
        String messageId,

        @Pattern(regexp = "like|dislike|bookmark|share", message = "reactionType must be like, dislike, bookmark, or share")
        @Schema(description = "Reaction type", example = "like")
        String reactionType,

        @Min(0) @Max(200)
        @Schema(description = "Position in feed when reaction occurred", example = "1")
        Integer positionInFeed,

        @PositiveOrZero
        @Schema(description = "Dwell time before the reaction (ms)", example = "1700")
        Integer dwellTimeBeforeMs
    ) { }
}

