package com.contentaggregation.metrics.kafka.consumer;

import com.contentaggregation.metrics.config.KafkaConfig;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.monitoring.ProfileProcessingLagMonitor;
import com.contentaggregation.metrics.service.profile.ProfileBuilderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ProfileEventsListener {

    private static final Logger log = LoggerFactory.getLogger(ProfileEventsListener.class);

    private final ObjectMapper objectMapper;
    private final ProfileBuilderService profileBuilderService;
    private final ProfileProcessingLagMonitor lagMonitor;
    private final MeterRegistry meterRegistry;

    public ProfileEventsListener(
        ObjectMapper objectMapper,
        ProfileBuilderService profileBuilderService,
        ProfileProcessingLagMonitor lagMonitor,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.profileBuilderService = profileBuilderService;
        this.lagMonitor = lagMonitor;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
        id = "profile-events-consumer",
        containerFactory = "profileEventsListenerContainerFactory",
        topics = {
            KafkaConfig.USER_EVENTS_ENGAGEMENT,
            KafkaConfig.USER_EVENTS_REACTIONS
        }
    )
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (records == null || records.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        Map<Integer, List<ConsumerRecord<String, String>>> recordsByPartition = records.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ConsumerRecord::partition,
                LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));

        recordsByPartition.values()
            .parallelStream()
            .forEach(partitionRecords -> partitionRecords.stream()
                .sorted(Comparator.comparingLong(ConsumerRecord::offset))
                .map(this::deserializeEvent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(this::processEvent)
            );

        acknowledgment.acknowledge();
    }

    private Optional<UserEventPayload> deserializeEvent(ConsumerRecord<String, String> record) {
        try {
            return Optional.of(objectMapper.readValue(record.value(), UserEventPayload.class));
        } catch (Exception ex) {
            log.error("Failed to deserialize payload from topic {}: {}", record.topic(), ex.getMessage());
            return Optional.empty();
        }
    }

    private void processEvent(UserEventPayload event) {
        try {
            profileBuilderService.processEvent(event);
            recordMetrics(event);
        } catch (Exception ex) {
            log.error("Failed to process event {}: {}", event.base().eventId(), ex.getMessage());
            throw ex;
        }
    }

    private void recordMetrics(UserEventPayload event) {
        meterRegistry.counter("events.processed.total", "type", event.base().eventType().value()).increment();
        Instant timestamp = event.base().timestamp();
        if (timestamp != null) {
            long lagMillis = ChronoUnit.MILLIS.between(timestamp, Instant.now());
            lagMonitor.recordLag(lagMillis);
        }
    }
}


