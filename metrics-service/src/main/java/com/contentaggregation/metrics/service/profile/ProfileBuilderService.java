package com.contentaggregation.metrics.service.profile;

import com.contentaggregation.metrics.client.ContentServiceClient;
import com.contentaggregation.metrics.dto.event.PostClickEvent;
import com.contentaggregation.metrics.dto.event.PostDwellEvent;
import com.contentaggregation.metrics.dto.event.PostReactionEvent;
import com.contentaggregation.metrics.dto.event.PostViewEvent;
import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.entity.UserProfile;
import com.contentaggregation.metrics.repository.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ProfileBuilderService.class);

    private final UserProfileRepository userProfileRepository;
    private final ContentServiceClient contentServiceClient;

    public ProfileBuilderService(
        UserProfileRepository userProfileRepository,
        ContentServiceClient contentServiceClient
    ) {
        this.userProfileRepository = userProfileRepository;
        this.contentServiceClient = contentServiceClient;
    }

    @Transactional
    public void processEvent(UserEventPayload event) {
        if (event == null || event.base() == null) {
            return;
        }
        UUID userId = event.base().userId();
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseGet(() -> createProfile(userId));

        if (event instanceof PostViewEvent viewEvent) {
            handlePostView(profile, viewEvent);
        } else if (event instanceof PostClickEvent clickEvent) {
            handlePostClick(profile, clickEvent);
        } else if (event instanceof PostDwellEvent dwellEvent) {
            handlePostDwell(profile, dwellEvent);
        } else if (event instanceof PostReactionEvent reactionEvent) {
            handlePostReaction(profile, reactionEvent);
        }

        profile.setLastUpdated(LocalDateTime.now(ZoneOffset.UTC));
        userProfileRepository.save(profile);
    }

    private UserProfile createProfile(UUID userId) {
        return UserProfile.builder()
            .userId(userId)
            .lastUpdated(LocalDateTime.now(ZoneOffset.UTC))
            .totalViews(0)
            .totalClicks(0)
            .totalLikes(0)
            .totalDislikes(0)
            .totalBookmarks(0)
            .totalSessions(0)
            .build();
    }

    private void handlePostView(UserProfile profile, PostViewEvent event) {
        profile.setTotalViews(profile.getTotalViews() + 1);
        profile.addRecentView(event.data().messageId());
    }

    private void handlePostClick(UserProfile profile, PostClickEvent event) {
        profile.setTotalClicks(profile.getTotalClicks() + 1);
        profile.addRecentView(event.data().messageId());
    }

    private void handlePostDwell(UserProfile profile, PostDwellEvent event) {
        profile.updateAvgDwellTimeSeconds(event.data().dwellTimeMs());
        ContentServiceClient.ContentMetadata metadata = safeFetchMetadata(event.data().messageId());
        updateCategoryPreferences(profile, metadata.categories(), 0.2);
        updateEntityPreferences(profile, metadata.entities());
        profile.updateClickbaitTolerance(metadata.isClickbait(), event.data().scrollDepth() > 0.7);
    }

    private void handlePostReaction(UserProfile profile, PostReactionEvent event) {
        String reaction = Optional.ofNullable(event.data().reactionType()).orElse("");
        ContentServiceClient.ContentMetadata metadata = safeFetchMetadata(event.data().messageId());

        switch (reaction) {
            case "like" -> {
                profile.setTotalLikes(profile.getTotalLikes() + 1);
                updateCategoryPreferences(profile, metadata.categories(), 0.3);
                profile.addLikedPost(event.data().messageId());
            }
            case "dislike" -> {
                profile.setTotalDislikes(profile.getTotalDislikes() + 1);
                updateCategoryPreferences(profile, metadata.categories(), -0.15);
                profile.addDislikedPost(event.data().messageId());
            }
            case "bookmark" -> profile.setTotalBookmarks(profile.getTotalBookmarks() + 1);
            default -> {
            }
        }

        updateEntityPreferences(profile, metadata.entities());
    }

    private ContentServiceClient.ContentMetadata safeFetchMetadata(String messageId) {
        try {
            return contentServiceClient.getContentMetadata(messageId);
        } catch (Exception ex) {
            log.warn("Metadata unavailable for {}: {}", messageId, ex.getMessage());
            return ContentServiceClient.ContentMetadata.empty();
        }
    }

    private void updateCategoryPreferences(UserProfile profile, Map<String, Double> categories, double alpha) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        categories.forEach((category, score) -> profile.updateCategoryPreference(category, score, alpha));
    }

    private void updateEntityPreferences(UserProfile profile, Map<String, java.util.List<String>> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach((entityType, values) -> {
            if (values == null) {
                return;
            }
            values.forEach(value -> profile.updateEntityPreference(entityType, value));
        });
    }
}

