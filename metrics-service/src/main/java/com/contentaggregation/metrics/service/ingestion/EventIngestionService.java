package com.contentaggregation.metrics.service.ingestion;

import com.contentaggregation.metrics.dto.event.EventBatchRequest;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.dto.response.EventIngestionResponse;
import com.contentaggregation.metrics.dto.response.ValidationError;
import com.contentaggregation.metrics.entity.EventBatch;
import com.contentaggregation.metrics.entity.UserEvent;
import com.contentaggregation.metrics.kafka.producer.EventProducer;
import com.contentaggregation.metrics.repository.EventBatchRepository;
import com.contentaggregation.metrics.repository.UserEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final EventValidationService validationService;
    private final EventDeduplicationService deduplicationService;
    private final RateLimiterService rateLimiterService;
    private final EventProducer eventProducer;
    private final UserEventRepository userEventRepository;
    private final EventBatchRepository eventBatchRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public EventIngestionService(
        EventValidationService validationService,
        EventDeduplicationService deduplicationService,
        RateLimiterService rateLimiterService,
        EventProducer eventProducer,
        UserEventRepository userEventRepository,
        EventBatchRepository eventBatchRepository,
        ObjectMapper objectMapper,
        MeterRegistry meterRegistry
    ) {
        this.validationService = validationService;
        this.deduplicationService = deduplicationService;
        this.rateLimiterService = rateLimiterService;
        this.eventProducer = eventProducer;
        this.userEventRepository = userEventRepository;
        this.eventBatchRepository = eventBatchRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public EventIngestionResponse ingestEventBatch(EventBatchRequest request, UUID userId, UUID requestId) {
        Timer.Sample sample = Timer.start(meterRegistry);

        if (deduplicationService.isDuplicateBatch(request.batchId().toString())) {
            meterRegistry.counter("events.duplicates.total", "type", "batch").increment();
            return EventIngestionResponse.duplicate();
        }

        log.debug("Processing event batch {} for user {}", request.batchId(), userId);
        rateLimiterService.assertWithinLimit(userId, request.events().size());

        List<UserEventPayload> acceptedEvents = new ArrayList<>();
        List<ValidationError> errors = new ArrayList<>();

        for (UserEventPayload event : request.events()) {
            EventValidationService.ValidationResult validation = validationService.validate(event, userId);
            if (!validation.success()) {
                errors.add(validation.error());
                if (validation.error() != null) {
                    meterRegistry.counter("events.rejected.total", "field", validation.error().field()).increment();
                }
                continue;
            }

            if (deduplicationService.isDuplicateEvent(event.base().eventId().toString())) {
                ValidationError error = new ValidationError(
                    event.base().eventId().toString(),
                    "eventId",
                    "Duplicate event detected"
                );
                errors.add(error);
                meterRegistry.counter("events.duplicates.total", "type", "event").increment();
                continue;
            }

            acceptedEvents.add(event);
        }

        if (!acceptedEvents.isEmpty()) {
            saveEventsToDatabase(acceptedEvents, userId, requestId);
            eventProducer.publishEventBatch(acceptedEvents, userId);
            acceptedEvents.forEach(event ->
                meterRegistry.counter("events.ingested.total", "type", event.base().eventType().value()).increment()
            );
        }

        saveBatchRecord(request, userId, acceptedEvents.size());

        EventIngestionResponse response = EventIngestionResponse.success(acceptedEvents.size(), errors.size(), errors);
        sample.stop(meterRegistry.timer("events.ingestion.latency", "status", response.status()));
        return response;
    }

    private void saveEventsToDatabase(List<UserEventPayload> events, UUID userId, UUID requestId) {
        List<UserEvent> entities = events.stream()
            .map(event -> UserEvent.builder()
                .eventId(event.base().eventId())
                .userId(event.base().userId())
                .sessionId(event.base().sessionId())
                .eventType(event.base().eventType().value())
                .eventData(serialize(event))
                .timestamp(LocalDateTime.ofInstant(event.base().timestamp(), ZoneOffset.UTC))
                .platform(event.base().platform())
                .appVersion(event.base().appVersion())
                .requestId(requestId)
                .build())
            .toList();

        userEventRepository.saveAll(entities);
    }

    private void saveBatchRecord(EventBatchRequest request, UUID userId, int acceptedCount) {
        EventBatch batch = EventBatch.builder()
            .batchId(request.batchId())
            .userId(userId)
            .eventCount(acceptedCount)
            .receivedAt(LocalDateTime.ofInstant(request.timestamp(), ZoneOffset.UTC))
            .processed(Boolean.TRUE)
            .build();

        eventBatchRepository.save(batch);
    }

    private String serialize(UserEventPayload event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize event payload", e);
        }
    }
}

