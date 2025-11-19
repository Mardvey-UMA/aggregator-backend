package com.contentaggregation.metrics.kafka;

import com.contentaggregation.metrics.MetricsServiceApplication;
import com.contentaggregation.metrics.client.ContentServiceClient;
import com.contentaggregation.metrics.dto.event.BaseEvent;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.entity.UserProfile;
import com.contentaggregation.metrics.repository.UserProfileRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MetricsServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"user-events-engagement", "user-events-reactions"})
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.streams.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "app.kafka.streams.application-id=metrics-service-streams-test",
    "app.kafka.streams.processing-guarantee=at_least_once"
})
class ProfileUpdateIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withReuse(false);

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockBean
    private ContentServiceClient contentServiceClient;

    @Test
    void shouldUpdateProfileWhenReactionEventConsumed() throws Exception {
        UUID userId = UUID.randomUUID();

        when(contentServiceClient.getContentMetadata(anyString()))
            .thenReturn(new ContentServiceClient.ContentMetadata(
                Map.of("science", 0.8),
                Map.of("people", List.of("grace")),
                "positive",
                false,
                "article"
            ));

        PostReactionEvent event = new PostReactionEvent(
            new BaseEvent(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                Instant.now(),
                "web",
                "1.0.0",
                BaseEvent.EventType.POST_REACTION
            ),
            new PostReactionEvent.PostReactionData("content-123", "like", 0, 1000)
        );

        kafkaTemplate.send("user-events-reactions", userId.toString(), event).get(5, TimeUnit.SECONDS);

        Awaitility.await()
            .atMost(45, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
            assertThat(profile).isNotNull();
            assertThat(profile.getTotalLikes()).isGreaterThanOrEqualTo(1);
            assertThat(profile.getCategoryPreferencesMap()).containsKey("science");
        });
    }
}

