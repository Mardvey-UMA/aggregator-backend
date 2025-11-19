package com.contentaggregation.metrics.kafka.consumer;

import com.contentaggregation.metrics.config.KafkaConfig;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.service.profile.ProfileBuilderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileUpdateConsumer.class);

    private final ProfileBuilderService profileBuilderService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RetryTemplate profileRetryTemplate;

    public ProfileUpdateConsumer(
        ProfileBuilderService profileBuilderService,
        ObjectMapper objectMapper,
        KafkaTemplate<String, Object> kafkaTemplate,
        RetryTemplate profileRetryTemplate
    ) {
        this.profileBuilderService = profileBuilderService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.profileRetryTemplate = profileRetryTemplate;
    }

    @Bean
    public KStream<String, String> profileUpdateStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(List.of(
            KafkaConfig.USER_EVENTS_ENGAGEMENT,
            KafkaConfig.USER_EVENTS_REACTIONS
        ));

        stream.filter((key, value) -> value != null)
            .foreach(this::processRecord);

        return stream;
    }

    private void processRecord(String key, String payload) {
        UserEventPayload event;
        try {
            event = objectMapper.readValue(payload, UserEventPayload.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize event payload: {}", ex.getMessage());
            kafkaTemplate.send(KafkaConfig.USER_PROFILE_UPDATES_DLT, key, payload);
            return;
        }

        profileRetryTemplate.execute(context -> {
            try {
                profileBuilderService.processEvent(event);
            } catch (Exception ex) {
                log.warn("Profile update failed on attempt {}: {}", context.getRetryCount() + 1, ex.getMessage());
                throw ex;
            }
            return null;
        }, context -> {
            log.error("Profile update permanently failed for key {}: {}", key, context.getLastThrowable().getMessage());
            kafkaTemplate.send(KafkaConfig.USER_PROFILE_UPDATES_DLT, key, payload);
            return null;
        });
    }
}

