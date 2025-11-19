package com.contentaggregation.content.entity;

import com.contentaggregation.content.enums.Sentiment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing metadata for a content post.
 *
 * <p>Metadata includes preprocessed features such as categories, entities, keywords,
 * sentiment analysis, and readability metrics.
 */
@Entity
@Table(name = "content_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentMetadata {

    private static final Logger log = LoggerFactory.getLogger(ContentMetadata.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_post_id", nullable = false, unique = true)
    private ContentPost contentPost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String categories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String entities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String keywords;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    @Column(name = "sentiment_score")
    private Float sentimentScore;

    @Column(length = 10)
    private String language;

    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    @Column(name = "complexity_score")
    private Float complexityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Gets the primary category with the highest score.
     *
     * @return the primary category name, or null if no categories exist
     */
    public String getPrimaryCategory() {
        if (categories == null || categories.isEmpty()) {
            return null;
        }

        try {
            Map<String, Double> categoryMap = objectMapper.readValue(
                    categories,
                    new TypeReference<Map<String, Double>>() {}
            );

            return categoryMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse categories JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the content has a specific category.
     *
     * @param category the category to check
     * @return true if the category exists in the metadata
     */
    public boolean hasCategory(String category) {
        if (categories == null || categories.isEmpty() || category == null) {
            return false;
        }

        try {
            Map<String, Double> categoryMap = objectMapper.readValue(
                    categories,
                    new TypeReference<Map<String, Double>>() {}
            );

            return categoryMap.containsKey(category);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse categories JSON: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the category score for a specific category.
     *
     * @param category the category to get the score for
     * @return the category score, or 0.0 if not found
     */
    public double getCategoryScore(String category) {
        if (categories == null || categories.isEmpty() || category == null) {
            return 0.0;
        }

        try {
            Map<String, Double> categoryMap = objectMapper.readValue(
                    categories,
                    new TypeReference<Map<String, Double>>() {}
            );

            return categoryMap.getOrDefault(category, 0.0);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse categories JSON: {}", e.getMessage());
            return 0.0;
        }
    }
}
