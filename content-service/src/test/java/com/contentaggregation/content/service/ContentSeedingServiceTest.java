package com.contentaggregation.content.service;

import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.enums.Sentiment;
import com.contentaggregation.content.repository.ContentPostRepository;
import com.contentaggregation.content.service.content.ContentSeedingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ContentSeedingService.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ContentSeedingServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private ContentSeedingService seedingService;

    @Autowired
    private ContentPostRepository contentPostRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setup() {
        contentPostRepository.deleteAll();
    }

    @AfterEach
    void cleanup() {
        contentPostRepository.deleteAll();
    }

    @Test
    @DisplayName("Seeds requested number of items")
    void seedContent_seedsRequestedNumber() {
        int requestedCount = 50;
        int seededCount = seedingService.seedContent(requestedCount);

        assertEquals(requestedCount, seededCount);

        long actualCount = contentPostRepository.count();
        assertEquals(requestedCount, actualCount);
    }

    @Test
    @DisplayName("Seeds smaller batch correctly")
    void seedContent_smallBatch() {
        int requestedCount = 10;
        int seededCount = seedingService.seedContent(requestedCount);

        assertEquals(requestedCount, seededCount);
        assertEquals(requestedCount, contentPostRepository.count());
    }

    @Test
    @DisplayName("Seeds larger batch correctly")
    void seedContent_largerBatch() {
        int requestedCount = 100;
        int seededCount = seedingService.seedContent(requestedCount);

        assertEquals(requestedCount, seededCount);
        assertEquals(requestedCount, contentPostRepository.count());
    }

    @Test
    @DisplayName("Content distributed across categories")
    void seedContent_distributedAcrossCategories() throws Exception {
        seedingService.seedContent(100);

        List<ContentPost> posts = contentPostRepository.findAll();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (ContentPost post : posts) {
            if (post.getMetadata() != null && post.getMetadata().getCategories() != null) {
                Map<String, Double> categories = objectMapper.readValue(
                        post.getMetadata().getCategories(),
                        new TypeReference<Map<String, Double>>() {});

                // Get primary category (highest score)
                String primaryCategory = categories.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("unknown");

                categoryCounts.merge(primaryCategory, 1, Integer::sum);
            }
        }

        // Should have at least 3 different categories
        assertTrue(categoryCounts.size() >= 3,
                "Should have at least 3 different categories, found: " + categoryCounts.keySet());

        // No single category should have more than 50% of content
        int maxCount = categoryCounts.values().stream().max(Integer::compareTo).orElse(0);
        assertTrue(maxCount <= 50,
                "No category should dominate (max 50%), found max: " + maxCount);
    }

    @Test
    @DisplayName("Media types varied")
    void seedContent_mediaTypesVaried() {
        seedingService.seedContent(100);

        List<ContentPost> posts = contentPostRepository.findAll();

        long withMedia = posts.stream().filter(ContentPost::getHasMedia).count();
        long withoutMedia = posts.stream().filter(p -> !p.getHasMedia()).count();

        // Should have both with and without media
        assertTrue(withMedia > 0, "Should have some posts with media");
        assertTrue(withoutMedia > 0, "Should have some posts without media");

        // Count media types
        Map<String, Integer> mediaTypeCounts = new HashMap<>();
        for (ContentPost post : posts) {
            if (post.getMediaType() != null) {
                mediaTypeCounts.merge(post.getMediaType().name(), 1, Integer::sum);
            }
        }

        // Should have at least 2 different media types
        assertTrue(mediaTypeCounts.size() >= 2,
                "Should have at least 2 different media types, found: " + mediaTypeCounts.keySet());
    }

    @Test
    @DisplayName("Content types distributed")
    void seedContent_contentTypesDistributed() {
        seedingService.seedContent(100);

        List<ContentPost> posts = contentPostRepository.findAll();
        Map<String, Integer> contentTypeCounts = new HashMap<>();

        for (ContentPost post : posts) {
            if (post.getContentType() != null) {
                contentTypeCounts.merge(post.getContentType().name(), 1, Integer::sum);
            }
        }

        // Should have at least 3 different content types
        assertTrue(contentTypeCounts.size() >= 3,
                "Should have at least 3 different content types, found: " + contentTypeCounts.keySet());
    }

    @Test
    @DisplayName("Sentiment scores in valid range")
    void seedContent_sentimentScoresValid() {
        seedingService.seedContent(50);

        List<ContentPost> posts = contentPostRepository.findAll();

        for (ContentPost post : posts) {
            if (post.getMetadata() != null) {
                Sentiment sentiment = post.getMetadata().getSentiment();
                assertNotNull(sentiment, "Sentiment should not be null");

                Float sentimentScore = post.getMetadata().getSentimentScore();
                if (sentimentScore != null) {
                    assertTrue(sentimentScore >= -1.0f && sentimentScore <= 1.0f,
                            "Sentiment score should be between -1 and 1, got: " + sentimentScore);

                    // Verify score matches sentiment
                    if (sentiment == Sentiment.POSITIVE) {
                        assertTrue(sentimentScore >= 0,
                                "Positive sentiment should have score >= 0");
                    } else if (sentiment == Sentiment.NEGATIVE) {
                        assertTrue(sentimentScore <= 0,
                                "Negative sentiment should have score <= 0");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("All posts have required fields")
    void seedContent_allPostsHaveRequiredFields() {
        seedingService.seedContent(50);

        List<ContentPost> posts = contentPostRepository.findAll();

        for (ContentPost post : posts) {
            assertNotNull(post.getId(), "ID should not be null");
            assertNotNull(post.getExternalId(), "External ID should not be null");
            assertNotNull(post.getSource(), "Source should not be null");
            assertNotNull(post.getContent(), "Content should not be null");
            assertNotNull(post.getContentType(), "Content type should not be null");
            assertNotNull(post.getPublishedAt(), "Published at should not be null");
            assertNotNull(post.getContentLength(), "Content length should not be null");
            assertTrue(post.getContentLength() > 0, "Content length should be positive");
        }
    }

    @Test
    @DisplayName("External IDs are unique")
    void seedContent_externalIdsUnique() {
        seedingService.seedContent(50);

        List<ContentPost> posts = contentPostRepository.findAll();
        List<String> externalIds = posts.stream()
                .map(ContentPost::getExternalId)
                .toList();

        long uniqueCount = externalIds.stream().distinct().count();
        assertEquals(externalIds.size(), uniqueCount, "All external IDs should be unique");
    }

    @Test
    @DisplayName("Metadata created for all posts")
    void seedContent_metadataCreatedForAll() throws Exception {
        seedingService.seedContent(50);

        List<ContentPost> posts = contentPostRepository.findAll();

        for (ContentPost post : posts) {
            assertNotNull(post.getMetadata(), "Metadata should not be null for post: " + post.getId());
            assertNotNull(post.getMetadata().getCategories(), "Categories should not be null");
            assertNotNull(post.getMetadata().getSentiment(), "Sentiment should not be null");

            // Verify categories is valid JSON
            String categoriesJson = post.getMetadata().getCategories();
            Map<String, Double> categories = objectMapper.readValue(
                    categoriesJson, new TypeReference<Map<String, Double>>() {});
            assertFalse(categories.isEmpty(), "Categories should not be empty");

            // Verify category scores are valid
            for (Map.Entry<String, Double> entry : categories.entrySet()) {
                assertTrue(entry.getValue() >= 0 && entry.getValue() <= 1,
                        "Category score should be between 0 and 1");
            }
        }
    }

    @Test
    @DisplayName("Reading time calculated correctly")
    void seedContent_readingTimeCalculated() {
        seedingService.seedContent(30);

        List<ContentPost> posts = contentPostRepository.findAll();

        for (ContentPost post : posts) {
            if (post.getMetadata() != null) {
                Integer readingTime = post.getMetadata().getReadingTimeMinutes();
                assertNotNull(readingTime, "Reading time should not be null");
                assertTrue(readingTime >= 1, "Reading time should be at least 1 minute");
            }
        }
    }

    @Test
    @DisplayName("Sources distributed across types")
    void seedContent_sourcesDistributed() {
        seedingService.seedContent(100);

        List<ContentPost> posts = contentPostRepository.findAll();
        Map<String, Integer> sourceCounts = new HashMap<>();

        for (ContentPost post : posts) {
            if (post.getSource() != null) {
                sourceCounts.merge(post.getSource().name(), 1, Integer::sum);
            }
        }

        // Should have at least 2 different sources
        assertTrue(sourceCounts.size() >= 2,
                "Should have at least 2 different sources, found: " + sourceCounts.keySet());
    }

    @Test
    @DisplayName("Published dates are recent")
    void seedContent_publishedDatesRecent() {
        seedingService.seedContent(50);

        List<ContentPost> posts = contentPostRepository.findAll();
        java.time.LocalDateTime oneWeekAgo = java.time.LocalDateTime.now().minusWeeks(1);

        for (ContentPost post : posts) {
            assertTrue(post.getPublishedAt().isAfter(oneWeekAgo),
                    "Published date should be within last week");
        }
    }
}
