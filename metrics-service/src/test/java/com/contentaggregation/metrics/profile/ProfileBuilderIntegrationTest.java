package com.contentaggregation.metrics.profile;

import com.contentaggregation.metrics.client.ContentServiceClient;
import com.contentaggregation.metrics.dto.event.BaseEvent;
import com.contentaggregation.metrics.dto.event.PostDwellEvent;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.entity.UserProfile;
import com.contentaggregation.metrics.repository.UserProfileRepository;
import com.contentaggregation.metrics.support.AbstractIntegrationTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.contentaggregation.metrics.config.KafkaConfig.USER_EVENTS_ENGAGEMENT;
import static com.contentaggregation.metrics.config.KafkaConfig.USER_EVENTS_REACTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ProfileBuilderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockBean
    private ContentServiceClient contentServiceClient;

    private UUID userId;

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();
        userProfileRepository.deleteAll();
    }

    @Test
    void shouldUpdateProfileWhenReactionConsumed() throws Exception {
        when(contentServiceClient.getContentMetadata(anyString()))
            .thenReturn(new ContentServiceClient.ContentMetadata(
                Map.of("science", 0.8),
                Map.of("topics", List.of("ai")),
                "positive",
                false,
                "long_form"
            ));

        PostReactionEvent reactionEvent = new PostReactionEvent(
            baseEvent(BaseEvent.EventType.POST_REACTION),
            new PostReactionEvent.PostReactionData("content-1", "like", 0, 800)
        );

        kafkaTemplate.send(USER_EVENTS_REACTIONS, userId.toString(), reactionEvent).get(10, TimeUnit.SECONDS);

        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
                assertThat(profile).isNotNull();
                assertThat(profile.getTotalLikes()).isGreaterThanOrEqualTo(1);
                assertThat(profile.getCategoryPreferencesMap()).containsKey("science");
            });
    }

    @Test
    void shouldAggregateCategoriesAndEntitiesFromDwellEvents() throws Exception {
        when(contentServiceClient.getContentMetadata("deep-dive"))
            .thenReturn(new ContentServiceClient.ContentMetadata(
                Map.of("technology", 0.9, "ethics", 0.7),
                Map.of("people", List.of("ada", "alan")),
                "neutral",
                true,
                "article"
            ));

        PostDwellEvent dwellEvent = new PostDwellEvent(
            baseEvent(BaseEvent.EventType.POST_DWELL),
            new PostDwellEvent.PostDwellData("deep-dive", 9000, 0.9, 3)
        );

        kafkaTemplate.send(USER_EVENTS_ENGAGEMENT, userId.toString(), dwellEvent).get(10, TimeUnit.SECONDS);

        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
                assertThat(profile).isNotNull();
                assertThat(profile.getCategoryPreferencesMap()).containsKeys("technology", "ethics");
                assertThat(profile.getEntityPreferenceCounters())
                    .containsKey("people");
                assertThat(profile.getEntityPreferenceCounters().get("people"))
                    .containsEntry("ada", 1);
                assertThat(profile.getAvgDwellTimeSeconds()).isGreaterThan(0.0f);
            });
    }

    private BaseEvent baseEvent(BaseEvent.EventType type) {
        return new BaseEvent(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            Instant.now(),
            "web",
            "1.0.0",
            type
        );
    }
}


