package com.contentaggregation.metrics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
public class UserProfile {

    private static final int RECENT_VIEWS_LIMIT = 50;
    private static final int LIKED_POSTS_LIMIT = 50;
    private static final int DISLIKED_POSTS_LIMIT = 50;

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NotNull
    @Column(nullable = false, unique = true)
    private UUID userId;

    private LocalDateTime lastUpdated;

    @Builder.Default
    private Integer totalViews = 0;
    @Builder.Default
    private Integer totalClicks = 0;
    @Builder.Default
    private Integer totalLikes = 0;
    @Builder.Default
    private Integer totalDislikes = 0;
    @Builder.Default
    private Integer totalBookmarks = 0;
    @Builder.Default
    private Integer totalSessions = 0;

    @Builder.Default
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> categoryPreferences = new HashMap<>();

    @Builder.Default
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Map<String, Integer>> entityPreferences = new HashMap<>();

    @Builder.Default
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> contentTypePreferences = new HashMap<>();

    @Builder.Default
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> stylePreferences = new HashMap<>();

    @Builder.Default
    private Float avgDwellTimeSeconds = 0.0f;
    @Builder.Default
    private Integer avgSessionLengthSeconds = 0;

    @Builder.Default
    @Column(columnDefinition = "integer[]")
    private Integer[] activeHours = new Integer[0];

    private String preferredContentLength;

    @Builder.Default
    private Float clickbaitTolerance = 0.5f;

    @Builder.Default
    private Float explorationVsExploitation = 0.5f;

    @Builder.Default
    @Column(columnDefinition = "text[]")
    private String[] likedPostsLast30d = new String[0];

    @Builder.Default
    @Column(columnDefinition = "text[]")
    private String[] dislikedPostsLast30d = new String[0];

    @Builder.Default
    @Column(columnDefinition = "text[]")
    private String[] viewedPostsLast7d = new String[0];

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamps() {
        lastUpdated = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Map<String, Double> getCategoryPreferencesMap() {
        return Map.copyOf(categoryPreferences);
    }

    public Map<String, Object> getEntityPreferencesMap() {
        Map<String, Object> copy = new HashMap<>();
        entityPreferences.forEach(copy::put);
        return Map.copyOf(copy);
    }

    public Map<String, Object> getContentTypePreferencesMap() {
        return Map.copyOf(contentTypePreferences);
    }

    public Map<String, Object> getStylePreferencesMap() {
        return Map.copyOf(stylePreferences);
    }

    public Map<String, Map<String, Integer>> getEntityPreferenceCounters() {
        return Map.copyOf(entityPreferences);
    }

    public void updateCategoryPreference(String category, double newScore, double alpha) {
        if (category == null || category.isBlank()) {
            return;
        }
        double currentScore = categoryPreferences.getOrDefault(category, 0.0);
        double weight = Math.min(1.0, Math.max(0.0, Math.abs(alpha)));
        double signedScore = Math.signum(alpha == 0 ? 1 : alpha) * newScore;
        double updatedScore = weight * signedScore + (1 - weight) * currentScore;
        categoryPreferences.put(category, Math.min(1.0, Math.max(0.0, updatedScore)));
    }

    public void updateEntityPreference(String entityType, String entityName) {
        if (entityType == null || entityType.isBlank() || entityName == null || entityName.isBlank()) {
            return;
        }
        Map<String, Integer> entityCounts = entityPreferences.computeIfAbsent(entityType, key -> new HashMap<>());
        entityCounts.merge(entityName, 1, Integer::sum);
    }

    public void addRecentView(String messageId) {
        viewedPostsLast7d = appendUnique(viewedPostsLast7d, messageId, RECENT_VIEWS_LIMIT);
    }

    public void addLikedPost(String messageId) {
        likedPostsLast30d = appendUnique(likedPostsLast30d, messageId, LIKED_POSTS_LIMIT);
    }

    public void addDislikedPost(String messageId) {
        dislikedPostsLast30d = appendUnique(dislikedPostsLast30d, messageId, DISLIKED_POSTS_LIMIT);
    }

    public void updateAvgDwellTimeSeconds(int dwellTimeMs) {
        double dwellSeconds = dwellTimeMs / 1000.0;
        if (avgDwellTimeSeconds == null || avgDwellTimeSeconds == 0.0f) {
            avgDwellTimeSeconds = (float) dwellSeconds;
        } else {
            avgDwellTimeSeconds = (float) ((avgDwellTimeSeconds * 0.8) + (dwellSeconds * 0.2));
        }
    }

    public void updateClickbaitTolerance(boolean isClickbait, boolean completed) {
        float delta = 0.0f;
        if (isClickbait && completed) {
            delta = 0.05f;
        } else if (!isClickbait && completed) {
            delta = -0.02f;
        }
        clickbaitTolerance = clamp(clickbaitTolerance + delta);
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private String[] appendUnique(String[] source, String value, int limit) {
        if (value == null || value.isBlank()) {
            return source == null ? new String[0] : source;
        }
        List<String> list = new ArrayList<>();
        list.add(value);
        if (source != null) {
            for (String existing : source) {
                if (!value.equals(existing) && list.size() < limit) {
                    list.add(existing);
                }
            }
        }
        return list.toArray(new String[0]);
    }

}

