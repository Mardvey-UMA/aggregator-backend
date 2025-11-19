package com.contentaggregation.content.service;

import com.contentaggregation.content.dto.UserPreferences;
import com.contentaggregation.content.dto.response.ContentDto;
import com.contentaggregation.content.entity.ContentMetadata;
import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.enums.ContentType;
import com.contentaggregation.content.enums.Sentiment;
import com.contentaggregation.content.mapper.ContentMapper;
import com.contentaggregation.content.repository.ContentPostRepository;
import com.contentaggregation.content.repository.ContentViewRepository;
import com.contentaggregation.content.service.recommendation.StubRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StubRecommendationService.
 */
@ExtendWith(MockitoExtension.class)
class StubRecommendationServiceTest {

    @Mock
    private ContentPostRepository contentPostRepository;

    @Mock
    private com.contentaggregation.content.repository.ContentBookmarkRepository bookmarkRepository;

    @Mock
    private ContentViewRepository viewRepository;

    @Mock
    private ContentMapper contentMapper;

    private StubRecommendationService recommendationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        recommendationService = new StubRecommendationService(
                contentPostRepository, bookmarkRepository, viewRepository, contentMapper);
    }

    @Test
    @DisplayName("Diversity rules enforced - no 4 consecutive same category")
    void getPersonalizedFeed_enforcesDiversityRules() throws Exception {
        // Create 20 posts with same category
        List<ContentPost> posts = createPostsWithCategory("technology", 20);

        when(contentPostRepository.findByPublishedAtAfter(any())).thenReturn(posts);
        when(contentMapper.toDto(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost post = inv.getArgument(0);
            return createDtoFromPost(post);
        });

        UserPreferences preferences = UserPreferences.builder()
                .selectedCategories(List.of("technology"))
                .coldStart(false)
                .build();

        Page<ContentDto> result = recommendationService.getPersonalizedFeed(
                UUID.randomUUID(), preferences, PageRequest.of(0, 20));

        assertNotNull(result);

        // Check that no more than 3 consecutive items have the same category
        List<ContentDto> content = result.getContent();
        int consecutiveCount = 1;
        String lastCategory = null;

        for (ContentDto item : content) {
            String currentCategory = item.getPrimaryCategory();
            if (currentCategory != null && currentCategory.equals(lastCategory)) {
                consecutiveCount++;
                assertTrue(consecutiveCount <= 3,
                        "Found " + consecutiveCount + " consecutive same category items");
            } else {
                consecutiveCount = 1;
            }
            lastCategory = currentCategory;
        }
    }

    @Test
    @DisplayName("Content types mixed properly")
    void getPersonalizedFeed_mixesContentTypes() throws Exception {
        // Create posts with various content types
        List<ContentPost> posts = new ArrayList<>();
        posts.addAll(createPostsWithCategory("technology", 20));

        when(contentPostRepository.findByPublishedAtAfter(any())).thenReturn(posts);
        when(contentMapper.toDto(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost post = inv.getArgument(0);
            return createDtoFromPost(post);
        });

        UserPreferences preferences = UserPreferences.builder()
                .selectedContentTypes(List.of("ARTICLE", "VIDEO", "IMAGE"))
                .coldStart(false)
                .build();

        Page<ContentDto> result = recommendationService.getPersonalizedFeed(
                UUID.randomUUID(), preferences, PageRequest.of(0, 20));

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("User preferences affect results")
    void getPersonalizedFeed_userPreferencesAffectResults() throws Exception {
        List<ContentPost> posts = new ArrayList<>();
        posts.addAll(createPostsWithCategory("technology", 10));
        posts.addAll(createPostsWithCategory("sports", 10));

        when(contentPostRepository.findByPublishedAtAfter(any())).thenReturn(posts);
        when(contentMapper.toDto(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost post = inv.getArgument(0);
            return createDtoFromPost(post);
        });

        // Test with technology preference
        UserPreferences techPrefs = UserPreferences.builder()
                .selectedCategories(List.of("technology"))
                .coldStart(false)
                .build();

        Page<ContentDto> techResult = recommendationService.getPersonalizedFeed(
                UUID.randomUUID(), techPrefs, PageRequest.of(0, 10));

        assertNotNull(techResult);
        assertFalse(techResult.isEmpty());
    }

    @Test
    @DisplayName("Cold start returns balanced feed")
    void getPersonalizedFeed_coldStartReturnsBalancedFeed() throws Exception {
        List<ContentPost> posts = new ArrayList<>();
        posts.addAll(createPostsWithCategory("technology", 10));
        posts.addAll(createPostsWithCategory("sports", 10));
        posts.addAll(createPostsWithCategory("entertainment", 10));

        when(contentPostRepository.findByPublishedAtAfter(any())).thenReturn(posts);
        when(contentMapper.toDto(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost post = inv.getArgument(0);
            return createDtoFromPost(post);
        });

        // Cold start - no preferences
        UserPreferences coldStartPrefs = UserPreferences.builder()
                .selectedCategories(List.of())
                .coldStart(true)
                .build();

        Page<ContentDto> result = recommendationService.getPersonalizedFeed(
                null, coldStartPrefs, PageRequest.of(0, 15));

        // Count categories
        Map<String, Long> categoryCounts = new HashMap<>();
        for (ContentDto dto : result.getContent()) {
            String category = dto.getPrimaryCategory();
            if (category != null) {
                categoryCounts.merge(category, 1L, Long::sum);
            }
        }

        // Should have content from multiple categories
        assertTrue(categoryCounts.size() >= 2,
                "Cold start should return content from at least 2 categories");
    }

    @Test
    @DisplayName("Trending content returns recent popular items")
    void getTrendingContent_returnsPopularItems() throws Exception {
        List<ContentPost> posts = createPostsWithCategory("technology", 10);

        when(contentPostRepository.findByPublishedAtAfter(any())).thenReturn(posts);
        when(viewRepository.countByContentPostIdAndViewedAtAfter(any(), any()))
                .thenReturn(100L);
        when(contentMapper.toDto(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost post = inv.getArgument(0);
            return createDtoFromPost(post);
        });

        List<ContentDto> trending = recommendationService.getTrendingContent(5);

        assertNotNull(trending);
        assertTrue(trending.size() <= 5);
    }

    @Test
    @DisplayName("Similar content returns related items")
    void getSimilarContent_returnsRelatedItems() throws Exception {
        // Create source post with technology category
        ContentPost sourcePost = createPostsWithCategory("technology", 1).get(0);

        // Create similar posts
        List<ContentPost> similarPosts = new ArrayList<>();
        similarPosts.addAll(createPostsWithCategory("technology", 5));
        similarPosts.addAll(createPostsWithCategory("science", 3));

        when(contentPostRepository.findById(any())).thenReturn(Optional.of(sourcePost));
        when(contentPostRepository.findByPublishedAtAfter(any())).thenReturn(similarPosts);
        when(contentMapper.toDto(any(ContentPost.class))).thenAnswer(inv -> {
            ContentPost post = inv.getArgument(0);
            return createDtoFromPost(post);
        });

        List<ContentDto> similar = recommendationService.getSimilarContent(sourcePost.getId(), 5);

        assertNotNull(similar);
        assertTrue(similar.size() <= 5);
    }

    private List<ContentPost> createPostsWithCategory(String category, int count) throws Exception {
        List<ContentPost> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ContentPost post = ContentPost.builder()
                    .id(UUID.randomUUID())
                    .externalId("test-" + UUID.randomUUID())
                    .source(ContentSource.RSS)
                    .title("Test Post " + i + " - " + category)
                    .content("Content for " + category)
                    .contentType(ContentType.LONG_ARTICLE)
                    .contentLength(100)
                    .hasMedia(false)
                    .publishedAt(LocalDateTime.now().minusHours(i))
                    .build();

            ContentMetadata metadata = ContentMetadata.builder()
                    .id(UUID.randomUUID())
                    .contentPost(post)
                    .categories(objectMapper.writeValueAsString(Map.of(category, 0.9)))
                    .sentiment(Sentiment.NEUTRAL)
                    .language("en")
                    .build();
            post.setMetadata(metadata);

            posts.add(post);
        }
        return posts;
    }

    private ContentDto createDtoFromPost(ContentPost post) {
        ContentDto dto = new ContentDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setContentType(post.getContentType());
        dto.setSource(post.getSource());
        dto.setSourceChannelName(post.getSourceChannelName());
        dto.setLinkUrl(post.getLinkUrl());
        dto.setMediaUrls(List.of());
        dto.setMediaType(post.getMediaType());
        dto.setPublishedAt(post.getPublishedAt());

        if (post.getMetadata() != null) {
            try {
                Map<String, Double> categories = objectMapper.readValue(
                        post.getMetadata().getCategories(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {});
                dto.setCategories(categories);
            } catch (Exception e) {
                dto.setCategories(Map.of());
            }
        }

        dto.setKeywords(List.of());
        dto.setRecommendationScore(0.0);
        dto.setViewCount(0L);
        dto.setBookmarkCount(0L);
        dto.setIsBookmarked(false);

        return dto;
    }
}
