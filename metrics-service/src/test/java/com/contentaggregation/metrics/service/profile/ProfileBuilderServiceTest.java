package com.contentaggregation.metrics.service.profile;

import com.contentaggregation.metrics.client.ContentServiceClient;
import com.contentaggregation.metrics.dto.event.BaseEvent;
import com.contentaggregation.metrics.dto.event.PostDwellEvent;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.dto.event.PostViewEvent;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.entity.UserProfile;
import com.contentaggregation.metrics.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileBuilderServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ContentServiceClient contentServiceClient;

    @InjectMocks
    private ProfileBuilderService profileBuilderService;

    @Captor
    private ArgumentCaptor<UserProfile> profileCaptor;

    private UUID userId;
    private UserProfile existingProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        existingProfile = UserProfile.builder()
            .userId(userId)
            .totalViews(10)
            .totalClicks(2)
            .totalLikes(3)
            .totalDislikes(1)
            .build();

        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldUpdateCategoryPreferencesForLikes() {
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(contentServiceClient.getContentMetadata("msg-1"))
            .thenReturn(new ContentServiceClient.ContentMetadata(
                Map.of("tech", 0.9),
                Map.of(),
                "positive",
                false,
                "article"
            ));

        UserEventPayload event = new PostReactionEvent(
            baseEvent(BaseEvent.EventType.POST_REACTION),
            new PostReactionEvent.PostReactionData("msg-1", "like", 1, 500)
        );

        profileBuilderService.processEvent(event);

        assertThat(existingProfile.getTotalLikes()).isEqualTo(4);
        assertThat(existingProfile.getCategoryPreferencesMap().get("tech")).isNotNull();
    }

    @Test
    void shouldUpdateEntityCountsAndDwellAverage() {
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(contentServiceClient.getContentMetadata("msg-2"))
            .thenReturn(new ContentServiceClient.ContentMetadata(
                Map.of("science", 0.7),
                Map.of("people", List.of("alice", "bob")),
                "neutral",
                true,
                "article"
            ));

        UserEventPayload event = new PostDwellEvent(
            baseEvent(BaseEvent.EventType.POST_DWELL),
            new PostDwellEvent.PostDwellData("msg-2", 8000, 0.8, 2)
        );

        profileBuilderService.processEvent(event);

        assertThat(existingProfile.getCategoryPreferencesMap()).containsKey("science");
        assertThat(existingProfile.getEntityPreferenceCounters().get("people").get("alice")).isEqualTo(1);
        assertThat(existingProfile.getAvgDwellTimeSeconds()).isGreaterThan(0);
        assertThat(existingProfile.getClickbaitTolerance()).isGreaterThan(0.5f);
    }

    @Test
    void shouldTrackRecentViews() {
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        UserEventPayload event = new PostViewEvent(
            baseEvent(BaseEvent.EventType.POST_VIEW),
            new PostViewEvent.PostViewData("msg-3", 0, 2000, 0.9)
        );

        profileBuilderService.processEvent(event);

        assertThat(existingProfile.getViewedPostsLast7d()).contains("msg-3");
        assertThat(existingProfile.getTotalViews()).isEqualTo(11);
    }

    private BaseEvent baseEvent(BaseEvent.EventType eventType) {
        return new BaseEvent(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            Instant.now(),
            "web",
            "1.0.0",
            eventType
        );
    }
}

