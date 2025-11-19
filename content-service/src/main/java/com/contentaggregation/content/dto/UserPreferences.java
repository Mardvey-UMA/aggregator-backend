package com.contentaggregation.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * User preferences for content recommendation.
 *
 * <p>Contains user-selected categories, content types, and calculated preference scores
 * used by the recommendation service to personalize content feeds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    /**
     * Categories selected by user during onboarding.
     * Examples: "technology", "science", "business", "entertainment"
     */
    private List<String> selectedCategories;

    /**
     * Content types preferred by user.
     * Examples: "SHORT_POST", "LONG_ARTICLE", "VIDEO_POST"
     */
    private List<String> selectedContentTypes;

    /**
     * Calculated category scores based on user behavior.
     * Key: category name, Value: score (0.0 to 1.0)
     */
    private Map<String, Double> categoryScores;

    /**
     * User's preferred language for content.
     */
    private String preferredLanguage;

    /**
     * Whether the user is a new user (cold start scenario).
     */
    @Builder.Default
    private boolean coldStart = true;

    /**
     * Creates default preferences for cold start users.
     *
     * @return default user preferences with no selected categories
     */
    public static UserPreferences defaultPreferences() {
        return UserPreferences.builder()
                .selectedCategories(List.of())
                .selectedContentTypes(List.of())
                .categoryScores(Map.of())
                .preferredLanguage("en")
                .coldStart(true)
                .build();
    }

    /**
     * Checks if user has any category preferences.
     *
     * @return true if user has selected categories or has category scores
     */
    public boolean hasPreferences() {
        return (selectedCategories != null && !selectedCategories.isEmpty()) ||
               (categoryScores != null && !categoryScores.isEmpty());
    }

    /**
     * Gets the score for a specific category.
     *
     * @param category the category to get score for
     * @return the category score, or 0.0 if not found
     */
    public double getCategoryScore(String category) {
        if (categoryScores == null || category == null) {
            return 0.0;
        }
        return categoryScores.getOrDefault(category.toLowerCase(), 0.0);
    }

    /**
     * Checks if user prefers a specific category.
     *
     * @param category the category to check
     * @return true if category is in selected categories
     */
    public boolean prefersCategory(String category) {
        if (selectedCategories == null || category == null) {
            return false;
        }
        return selectedCategories.stream()
                .anyMatch(c -> c.equalsIgnoreCase(category));
    }

    /**
     * Checks if user prefers a specific content type.
     *
     * @param contentType the content type to check
     * @return true if content type is in selected types
     */
    public boolean prefersContentType(String contentType) {
        if (selectedContentTypes == null || contentType == null) {
            return false;
        }
        return selectedContentTypes.stream()
                .anyMatch(t -> t.equalsIgnoreCase(contentType));
    }
}
