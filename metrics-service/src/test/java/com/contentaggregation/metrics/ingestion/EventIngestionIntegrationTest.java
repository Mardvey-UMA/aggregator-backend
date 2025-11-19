package com.contentaggregation.metrics.ingestion;

import com.contentaggregation.metrics.dto.event.BaseEvent;
import com.contentaggregation.metrics.dto.event.EventBatchRequest;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.dto.event.PostViewEvent;
import com.contentaggregation.metrics.dto.response.EventIngestionResponse;
import com.contentaggregation.metrics.dto.response.ValidationError;
import com.contentaggregation.metrics.repository.EventBatchRepository;
import com.contentaggregation.metrics.repository.UserEventRepository;
import com.contentaggregation.metrics.service.ingestion.EventIngestionService;
import com.contentaggregation.metrics.support.AbstractIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static com.contentaggregation.metrics.config.KafkaConfig.USER_EVENTS_ENGAGEMENT;
import static com.contentaggregation.metrics.config.KafkaConfig.USER_EVENTS_REACTIONS;
import static org.assertj.core.api.Assertions.assertThat;

class EventIngestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventIngestionService ingestionService;

    @Autowired
    private UserEventRepository userEventRepository;

    @Autowired
    private EventBatchRepository eventBatchRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEventRepository.deleteAll();
        eventBatchRepository.deleteAll();
    }

    @Test
    void shouldIngestEventBatchAndPublishToKafka() {
        EventBatchRequest request = new EventBatchRequest(
            UUID.randomUUID(),
            List.of(viewEvent(), reactionEvent()),
            Instant.now()
        );

        EventIngestionResponse response = ingestionService.ingestEventBatch(request, userId, UUID.randomUUID());

        assertThat(response.status()).isEqualTo("accepted");
        assertThat(response.acceptedEvents()).isEqualTo(2);
        assertThat(userEventRepository.count()).isEqualTo(2);
        assertThat(eventBatchRepository.count()).isEqualTo(1);

        try (KafkaConsumer<String, String> consumer = createConsumer("ingestion-test")) {
            consumer.subscribe(Set.of(USER_EVENTS_ENGAGEMENT, USER_EVENTS_REACTIONS));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(2);
        }
    }

    @Test
    void shouldRejectDuplicateEventIds() {
        UUID eventId = UUID.randomUUID();
        PostViewEvent event = new PostViewEvent(
            baseEvent(eventId, BaseEvent.EventType.POST_VIEW),
            new PostViewEvent.PostViewData("dup", 0, 1000, 0.8)
        );

        EventBatchRequest first = new EventBatchRequest(UUID.randomUUID(), List.of(event), Instant.now());
        EventBatchRequest duplicate = new EventBatchRequest(UUID.randomUUID(), List.of(event), Instant.now());

        EventIngestionResponse firstResponse = ingestionService.ingestEventBatch(first, userId, UUID.randomUUID());
        EventIngestionResponse duplicateResponse = ingestionService.ingestEventBatch(duplicate, userId, UUID.randomUUID());

        assertThat(firstResponse.acceptedEvents()).isEqualTo(1);
        assertThat(duplicateResponse.rejectedEvents()).isEqualTo(1);
        ValidationError error = duplicateResponse.errors().getFirst();
        assertThat(error.message()).contains("Duplicate event");
    }

    @Test
    void shouldReturnDuplicateStatusForRepeatedBatch() {
        EventBatchRequest request = new EventBatchRequest(
            UUID.randomUUID(),
            List.of(viewEvent()),
            Instant.now()
        );

        EventIngestionResponse first = ingestionService.ingestEventBatch(request, userId, UUID.randomUUID());
        EventIngestionResponse second = ingestionService.ingestEventBatch(request, userId, UUID.randomUUID());

        assertThat(first.status()).isEqualTo("accepted");
        assertThat(second.isDuplicate()).isTrue();
    }

    @Test
    void shouldReportValidationErrors() {
        UUID mismatchedUser = UUID.randomUUID();
        BaseEvent base = new BaseEvent(
            UUID.randomUUID(),
            mismatchedUser,
            UUID.randomUUID(),
            Instant.now(),
            "web",
            "1.0.0",
            BaseEvent.EventType.POST_VIEW
        );
        PostViewEvent invalidEvent = new PostViewEvent(base,
            new PostViewEvent.PostViewData("invalid", 0, 0, 2.0));

        EventBatchRequest request = new EventBatchRequest(UUID.randomUUID(), List.of(invalidEvent), Instant.now());

        EventIngestionResponse response = ingestionService.ingestEventBatch(request, userId, UUID.randomUUID());

        assertThat(response.status()).isEqualTo("partial");
        assertThat(response.rejectedEvents()).isEqualTo(1);
        assertThat(response.errors()).isNotEmpty();
        assertThat(response.errors().getFirst().field()).isIn("userId", "data");
    }

    private PostViewEvent viewEvent() {
        return new PostViewEvent(
            baseEvent(UUID.randomUUID(), BaseEvent.EventType.POST_VIEW),
            new PostViewEvent.PostViewData("post-1", 0, 1800, 0.95)
        );
    }

    private PostReactionEvent reactionEvent() {
        return new PostReactionEvent(
            baseEvent(UUID.randomUUID(), BaseEvent.EventType.POST_REACTION),
            new PostReactionEvent.PostReactionData("post-2", "like", 0, 1200)
        );
    }

    private BaseEvent baseEvent(UUID eventId, BaseEvent.EventType eventType) {
        return new BaseEvent(
            eventId,
            userId,
            UUID.randomUUID(),
            Instant.now(),
            "web",
            "1.0.0",
            eventType
        );
    }

    private KafkaConsumer<String, String> createConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new KafkaConsumer<>(props);
    }
}


