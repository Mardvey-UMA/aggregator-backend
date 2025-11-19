package com.contentaggregation.metrics.dto.response;

import com.contentaggregation.metrics.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Aggregated user profile used by the recommender service")
public record UserProfileDto(
    @Schema(description = "User identifier")
    UUID userId,

    @Schema(description = "Last time profile was recalculated")
    LocalDateTime lastUpdated,

    @Schema(description = "Total number of post impressions viewed")
    int totalViews,

    @Schema(description = "Total number of clicks")
    int totalClicks,

    @Schema(description = "Total number of likes")
    int totalLikes,

    @Schema(description = "Total number of dislikes")
    int totalDislikes,

    @Schema(description = "Total number of bookmarks")
    int totalBookmarks,

    @Schema(description = "Category preference weights between 0 and 1")
    Map<String, Double> categoryPreferences,

    @Schema(description = "Entity preference counters grouped by entity type")
    Map<String, Map<String, Integer>> entityPreferences,

    @Schema(description = "Content type preference weights between 0 and 1")
    Map<String, Double> contentTypePreferences,

    @Schema(description = "Rolling average dwell time in seconds")
    float avgDwellTimeSeconds,

    @Schema(description = "Average session duration in seconds")
    int avgSessionLengthSeconds,

    @Schema(description = "Hours of day (0-23) where user is active")
    List<Integer> activeHours,

    @Schema(description = "Preferred content length classification")
    String preferredContentLength,

    @Schema(description = "Clickbait tolerance score between 0 and 1")
    float clickbaitTolerance,

    @Schema(description = "Exploration vs exploitation score between 0 and 1")
    float explorationVsExploitation,

    @Schema(description = "Recently liked post identifiers")
    List<String> recentLikedPosts,

    @Schema(description = "Recently viewed post identifiers")
    List<String> recentViewedPosts
) {

    public static UserProfileDto fromEntity(UserProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        return new UserProfileDto(
            profile.getUserId(),
            profile.getLastUpdated(),
            profile.getTotalViews(),
            profile.getTotalClicks(),
            profile.getTotalLikes(),
            profile.getTotalDislikes(),
            profile.getTotalBookmarks(),
            profile.getCategoryPreferencesMap(),
            profile.getEntityPreferenceCounters(),
            toDoubleMap(profile.getContentTypePreferencesMap()),
            profile.getAvgDwellTimeSeconds() != null ? profile.getAvgDwellTimeSeconds() : 0.0f,
            profile.getAvgSessionLengthSeconds() != null ? profile.getAvgSessionLengthSeconds() : 0,
            toImmutableList(profile.getActiveHours()),
            profile.getPreferredContentLength(),
            profile.getClickbaitTolerance() != null ? profile.getClickbaitTolerance() : 0.0f,
            profile.getExplorationVsExploitation() != null ? profile.getExplorationVsExploitation() : 0.0f,
            toImmutableList(profile.getLikedPostsLast30d()),
            toImmutableList(profile.getViewedPostsLast7d())
        );
    }

    private static <T> List<T> toImmutableList(T[] source) {
        if (source == null || source.length == 0) {
            return List.of();
        }
        return List.copyOf(Arrays.asList(source));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> toDoubleMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return source.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    Object value = entry.getValue();
                    if (value instanceof Number number) {
                        return number.doubleValue();
                    }
                    if (value instanceof String text) {
                        try {
                            return Double.parseDouble(text);
                        } catch (NumberFormatException ex) {
                            return 0.0d;
                        }
                    }
                    return 0.0d;
                }
            ));
    }
}

