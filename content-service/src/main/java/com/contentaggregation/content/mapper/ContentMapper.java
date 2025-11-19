package com.contentaggregation.content.mapper;

import com.contentaggregation.content.dto.response.ContentDto;
import com.contentaggregation.content.entity.ContentMetadata;
import com.contentaggregation.content.entity.ContentPost;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for converting ContentPost entities to DTOs.
 */
@Component
public class ContentMapper {

    private static final Logger log = LoggerFactory.getLogger(ContentMapper.class);
    private final ObjectMapper objectMapper;

    public ContentMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a ContentPost entity to ContentDto.
     *
     * @param post the content post entity
     * @return the content DTO
     */
    public ContentDto toDto(ContentPost post) {
        if (post == null) {
            return null;
        }

        ContentDto.ContentDtoBuilder builder = ContentDto.builder()
                .id(post.getId())
                .externalId(post.getExternalId())
                .source(post.getSource())
                .sourceChannelId(post.getSourceChannelId())
                .sourceChannelName(post.getSourceChannelName())
                .title(post.getTitle())
                .content(post.getContent())
                .contentType(post.getContentType())
                .contentLength(post.getContentLength())
                .hasMedia(post.getHasMedia())
                .mediaType(post.getMediaType())
                .mediaUrls(parseMediaUrls(post.getMediaUrls()))
                .linkUrl(post.getLinkUrl())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt());

        // Add metadata if available
        if (post.getMetadata() != null) {
            ContentMetadata metadata = post.getMetadata();
            builder
                    .categories(parseCategories(metadata.getCategories()))
                    .entities(parseEntities(metadata.getEntities()))
                    .keywords(parseKeywords(metadata.getKeywords()))
                    .sentiment(metadata.getSentiment())
                    .sentimentScore(metadata.getSentimentScore())
                    .language(metadata.getLanguage())
                    .readingTimeMinutes(metadata.getReadingTimeMinutes())
                    .complexityScore(metadata.getComplexityScore());
        }

        return builder.build();
    }

    /**
     * Converts a ContentPost with recommendation data to ContentDto.
     *
     * @param post the content post entity
     * @param score recommendation score
     * @param reason recommendation reason
     * @return the content DTO with recommendation data
     */
    public ContentDto toDtoWithRecommendation(ContentPost post, double score, String reason) {
        ContentDto dto = toDto(post);
        if (dto != null) {
            dto.setRecommendationScore(score);
            dto.setRecommendationReason(reason);
            dto.setRecommendationId(UUID.randomUUID());
        }
        return dto;
    }

    /**
     * Converts a ContentPost with engagement metrics to ContentDto.
     *
     * @param post the content post entity
     * @param viewCount number of views
     * @param bookmarkCount number of bookmarks
     * @param isBookmarked whether current user has bookmarked
     * @return the content DTO with engagement data
     */
    public ContentDto toDtoWithEngagement(ContentPost post, long viewCount,
                                          long bookmarkCount, boolean isBookmarked) {
        ContentDto dto = toDto(post);
        if (dto != null) {
            dto.setViewCount(viewCount);
            dto.setBookmarkCount(bookmarkCount);
            dto.setIsBookmarked(isBookmarked);
        }
        return dto;
    }

    /**
     * Full conversion with all data.
     *
     * @param post the content post entity
     * @param score recommendation score
     * @param reason recommendation reason
     * @param viewCount number of views
     * @param bookmarkCount number of bookmarks
     * @param isBookmarked whether current user has bookmarked
     * @return the complete content DTO
     */
    public ContentDto toFullDto(ContentPost post, double score, String reason,
                                long viewCount, long bookmarkCount, boolean isBookmarked) {
        ContentDto dto = toDtoWithRecommendation(post, score, reason);
        if (dto != null) {
            dto.setViewCount(viewCount);
            dto.setBookmarkCount(bookmarkCount);
            dto.setIsBookmarked(isBookmarked);
        }
        return dto;
    }

    /**
     * Parses media URLs from JSON string.
     *
     * @param mediaUrlsJson JSON string of media URLs
     * @return list of media URLs
     */
    public List<String> parseMediaUrls(String mediaUrlsJson) {
        if (mediaUrlsJson == null || mediaUrlsJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(mediaUrlsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse media URLs: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses categories from JSON string.
     *
     * @param categoriesJson JSON string of categories
     * @return map of category names to scores
     */
    public Map<String, Double> parseCategories(String categoriesJson) {
        if (categoriesJson == null || categoriesJson.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(categoriesJson, new TypeReference<Map<String, Double>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse categories: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Parses entities from JSON string.
     *
     * @param entitiesJson JSON string of entities
     * @return map of entity types to entity lists
     */
    public Map<String, List<String>> parseEntities(String entitiesJson) {
        if (entitiesJson == null || entitiesJson.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(entitiesJson, new TypeReference<Map<String, List<String>>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse entities: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Parses keywords from JSON string.
     *
     * @param keywordsJson JSON string of keywords
     * @return list of keywords
     */
    public List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(keywordsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse keywords: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Converts a map to JSON string.
     *
     * @param map the map to convert
     * @return JSON string
     */
    public String toJson(Object map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert to JSON: {}", e.getMessage());
            return null;
        }
    }
}
