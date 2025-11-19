package com.contentaggregation.metrics.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

/**
 * Marker interface for all user interaction events handled by the metrics service.
 *
 * <p>Jackson polymorphism is configured to look at the {@code eventType} property (flattened from the
 * {@link BaseEvent}) in order to deserialize the correct record implementation.</p>
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "eventType",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PostImpressionEvent.class, name = BaseEvent.EventType.Names.POST_IMPRESSION),
    @JsonSubTypes.Type(value = PostViewEvent.class, name = BaseEvent.EventType.Names.POST_VIEW),
    @JsonSubTypes.Type(value = PostClickEvent.class, name = BaseEvent.EventType.Names.POST_CLICK),
    @JsonSubTypes.Type(value = PostDwellEvent.class, name = BaseEvent.EventType.Names.POST_DWELL),
    @JsonSubTypes.Type(value = PostReactionEvent.class, name = BaseEvent.EventType.Names.POST_REACTION),
    @JsonSubTypes.Type(value = SessionEvent.class, name = BaseEvent.EventType.Names.SESSION)
})
public sealed interface UserEventPayload permits
    PostImpressionEvent,
    PostViewEvent,
    PostClickEvent,
    PostDwellEvent,
    PostReactionEvent,
    SessionEvent {

    @Valid
    BaseEvent base();

    /**
     * Exposes the event type at the root JSON level so that Jackson can route to the appropriate subtype.
     */
    @JsonProperty("eventType")
    default String eventType() {
        return base().eventType().value();
    }

    /**
     * Enumeration of supported event types.
     */
    enum EventCategory {
        POST_IMPRESSION(BaseEvent.EventType.POST_IMPRESSION),
        POST_VIEW(BaseEvent.EventType.POST_VIEW),
        POST_CLICK(BaseEvent.EventType.POST_CLICK),
        POST_DWELL(BaseEvent.EventType.POST_DWELL),
        POST_REACTION(BaseEvent.EventType.POST_REACTION),
        SESSION(BaseEvent.EventType.SESSION);

        private final BaseEvent.EventType delegate;

        EventCategory(BaseEvent.EventType delegate) {
            this.delegate = delegate;
        }

        @JsonValue
        public String value() {
            return delegate.value();
        }

        public BaseEvent.EventType toEventType() {
            return delegate;
        }
    }
}

