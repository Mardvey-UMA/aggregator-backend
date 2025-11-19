package com.contentaggregation.metrics.dto.response;

import com.contentaggregation.metrics.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Representation of a user's aggregated preference profile")
public record UserProfileDto(
    UUID id,
    UUID userId,
    LocalDateTime lastUpdated,
    Integer totalViews,
    Integer totalClicks,
    Integer totalLikes,
    Integer totalDislikes,
    Integer totalBookmarks,
    Integer totalSessions,
    Map<String, Double> categoryPreferences,
    Map<String, Object> entityPreferences,
    Map<String, Object> contentTypePreferences,
    Map<String, Object> stylePreferences,
    Float avgDwellTimeSeconds,
    Integer avgSessionLengthSeconds,
    List<Integer> activeHours,
    String preferredContentLength,
    Float clickbaitTolerance,
    Float explorationVsExploitation,
    List<String> likedPostsLast30d,
    List<String> dislikedPostsLast30d,
    List<String> viewedPostsLast7d,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static UserProfileDto fromEntity(UserProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        return new UserProfileDto(
            profile.getId(),
            profile.getUserId(),
            profile.getLastUpdated(),
            profile.getTotalViews(),
            profile.getTotalClicks(),
            profile.getTotalLikes(),
            profile.getTotalDislikes(),
            profile.getTotalBookmarks(),
            profile.getTotalSessions(),
            profile.getCategoryPreferencesMap(),
            profile.getEntityPreferencesMap(),
            profile.getContentTypePreferencesMap(),
            profile.getStylePreferencesMap(),
            profile.getAvgDwellTimeSeconds(),
            profile.getAvgSessionLengthSeconds(),
            toImmutableList(profile.getActiveHours()),
            profile.getPreferredContentLength(),
            profile.getClickbaitTolerance(),
            profile.getExplorationVsExploitation(),
            toImmutableList(profile.getLikedPostsLast30d()),
            toImmutableList(profile.getDislikedPostsLast30d()),
            toImmutableList(profile.getViewedPostsLast7d()),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }

    private static <T> List<T> toImmutableList(T[] source) {
        if (source == null || source.length == 0) {
            return List.of();
        }
        return List.copyOf(Arrays.asList(source));
    }
}

