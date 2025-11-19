package com.contentaggregation.metrics.service.ingestion;

import com.contentaggregation.metrics.config.EventProcessingProperties;
import com.contentaggregation.metrics.dto.event.PostDwellEvent;
import com.contentaggregation.metrics.dto.event.PostImpressionEvent;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.dto.event.PostViewEvent;
import com.contentaggregation.metrics.dto.event.SessionEvent;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.dto.response.ValidationError;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventValidationService {

    private final EventProcessingProperties properties;

    public EventValidationService(EventProcessingProperties properties) {
        this.properties = properties;
    }

    public ValidationResult validate(UserEventPayload event, UUID expectedUserId) {
        if (event == null || event.base() == null) {
            return ValidationResult.invalid(new ValidationError(null, "base", "Event payload is missing base attributes"));
        }

        if (event.base().eventId() == null) {
            return ValidationResult.invalid(new ValidationError(null, "eventId", "Event identifier is required"));
        }

        if (!event.base().userId().equals(expectedUserId)) {
            return ValidationResult.invalid(new ValidationError(event.base().eventId().toString(), "userId", "Event user does not match authenticated user"));
        }

        Instant now = Instant.now();
        Instant timestamp = event.base().timestamp();
        if (timestamp == null) {
            return ValidationResult.invalid(new ValidationError(event.base().eventId().toString(), "timestamp", "Timestamp is required"));
        }

        if (timestamp.isAfter(now.plus(properties.getValidation().getMaxFutureSkew()))) {
            return ValidationResult.invalid(new ValidationError(event.base().eventId().toString(), "timestamp", "Event timestamp cannot be in the future"));
        }

        if (timestamp.isBefore(now.minus(properties.getValidation().getMaxAge()))) {
            return ValidationResult.invalid(new ValidationError(event.base().eventId().toString(), "timestamp", "Event is too old to ingest"));
        }

        if (!validateEventSpecifics(event)) {
            return ValidationResult.invalid(new ValidationError(event.base().eventId().toString(), "data", "Event data failed semantic validation"));
        }

        return ValidationResult.valid();
    }

    private boolean validateEventSpecifics(UserEventPayload event) {
        if (event instanceof PostImpressionEvent impression) {
            return impression.data().positionInFeed() >= 0 && impression.data().positionInFeed() <= 500;
        }

        if (event instanceof PostViewEvent view) {
            Double viewport = view.data().viewportPercentage();
            return viewport != null && viewport >= 0 && viewport <= 1;
        }

        if (event instanceof PostReactionEvent reaction) {
            return reaction.data().reactionType() != null;
        }

        if (event instanceof PostDwellEvent dwell) {
            return dwell.data().dwellTimeMs() != null && dwell.data().dwellTimeMs() > 0;
        }

        if (event instanceof SessionEvent session) {
            return session.data().durationMs() != null && session.data().durationMs() > 0;
        }

        return true;
    }

    public record ValidationResult(boolean success, ValidationError error) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(ValidationError error) {
            return new ValidationResult(false, error);
        }
    }
}

