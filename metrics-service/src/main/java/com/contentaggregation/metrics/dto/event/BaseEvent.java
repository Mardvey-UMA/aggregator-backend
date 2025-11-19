package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Common attributes shared by all metric events")
public record BaseEvent(
    @NotNull
    @Schema(description = "Unique event identifier", example = "6d7c2672-01f5-45a0-b800-501d0683be9c")
    UUID eventId,

    @NotNull
    @Schema(description = "Authenticated user identifier", example = "bf6c0a1b-68b2-4a57-9e0a-92d8478df4d1")
    UUID userId,

    @NotNull
    @Schema(description = "Session identifier", example = "5f6c670b-6d81-4c2a-a3d3-5c754cfa2dee")
    UUID sessionId,

    @NotNull
    @Schema(description = "Event timestamp in epoch milliseconds")
    Instant timestamp,

    @NotBlank
    @Pattern(regexp = "ios|android|web", message = "platform must be one of ios, android, web")
    @Schema(description = "Origin platform", example = "web")
    String platform,

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Application version", example = "1.4.0")
    String appVersion,

    @NotNull
    @Schema(description = "Event type discriminator", example = "post_impression")
    EventType eventType
) {

    public enum EventType {
        POST_IMPRESSION("post_impression"),
        POST_VIEW("post_view"),
        POST_CLICK("post_click"),
        POST_DWELL("post_dwell"),
        POST_REACTION("post_reaction"),
        SESSION("session"),
        SESSION_START("session_start"),
        SESSION_END("session_end"),
        POST_BOOKMARK("post_bookmark"),
        POST_SHARE("post_share");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static EventType fromValue(String value) {
            for (EventType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unsupported event type: " + value);
        }

        public static final class Names {
            public static final String POST_IMPRESSION = "post_impression";
            public static final String POST_VIEW = "post_view";
            public static final String POST_CLICK = "post_click";
            public static final String POST_DWELL = "post_dwell";
            public static final String POST_REACTION = "post_reaction";
            public static final String SESSION = "session";
            public static final String SESSION_START = "session_start";
            public static final String SESSION_END = "session_end";

            private Names() {
            }
        }
    }
}

