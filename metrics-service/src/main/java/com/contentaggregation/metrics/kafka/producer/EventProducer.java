package com.contentaggregation.metrics.kafka.producer;

import com.contentaggregation.metrics.config.KafkaConfig;
import com.contentaggregation.metrics.dto.event.PostClickEvent;
import com.contentaggregation.metrics.dto.event.PostDwellEvent;
import com.contentaggregation.metrics.dto.event.PostImpressionEvent;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.dto.event.PostViewEvent;
import com.contentaggregation.metrics.dto.event.SessionEvent;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(String topic, UUID userId, Object event) {
        kafkaTemplate.send(topic, userId.toString(), event);
    }

    public void publishEventBatch(List<UserEventPayload> events, UUID userId) {
        for (UserEventPayload event : events) {
            publishEvent(determineTopicForEvent(event), userId, event);
        }
    }

    private String determineTopicForEvent(Object event) {
        if (event instanceof PostImpressionEvent) {
            return KafkaConfig.USER_EVENTS_IMPRESSIONS;
        }
        if (event instanceof PostViewEvent || event instanceof PostClickEvent || event instanceof PostDwellEvent) {
            return KafkaConfig.USER_EVENTS_ENGAGEMENT;
        }
        if (event instanceof PostReactionEvent) {
            return KafkaConfig.USER_EVENTS_REACTIONS;
        }
        if (event instanceof SessionEvent) {
            return KafkaConfig.USER_EVENTS_SESSIONS;
        }
        return KafkaConfig.USER_EVENTS_ENGAGEMENT;
    }
}

