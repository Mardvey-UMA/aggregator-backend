package com.contentaggregation.content.service.recommendation;

import com.contentaggregation.content.dto.UserPreferences;
import com.contentaggregation.content.dto.response.ContentDto;
import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.mapper.ContentMapper;
import com.contentaggregation.content.repository.ContentBookmarkRepository;
import com.contentaggregation.content.repository.ContentPostRepository;
import com.contentaggregation.content.repository.ContentViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stub implementation of RecommendationService using rule-based algorithms.
 *
 * <p>Provides personalized, diverse content feeds using simple scoring and diversity rules.
 * This implementation simulates what an ML-based recommender would produce.
 */
@Service
@Transactional(readOnly = true)
public class StubRecommendationService implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(StubRecommendationService.class);

    private final ContentPostRepository contentPostRepository;
    private final ContentBookmarkRepository bookmarkRepository;
    private final ContentViewRepository viewRepository;
    private final ContentMapper contentMapper;

    private static final int MAX_CONSECUTIVE_SAME_CATEGORY = 3;
    private static final int MEDIA_INSERT_INTERVAL = 4;
    private static final double CATEGORY_MATCH_SCORE = 10.0;
    private static final double CONTENT_TYPE_MATCH_SCORE = 3.0;
    private static final double TRENDING_BONUS = 5.0;
    private static final double RANDOM_FACTOR_RANGE = 2.0;

    public StubRecommendationService(ContentPostRepository contentPostRepository,
                                      ContentBookmarkRepository bookmarkRepository,
                                      ContentViewRepository viewRepository,
                                      ContentMapper contentMapper) {
        this.contentPostRepository = contentPostRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.viewRepository = viewRepository;
        this.contentMapper = contentMapper;
    }

    @Override
    @Cacheable(value = "feed", key = "#userId + ':' + #pageable.pageNumber", unless = "#result.isEmpty()")
    public Page<ContentDto> getPersonalizedFeed(UUID userId, UserPreferences preferences, Pageable pageable) {
        log.debug("Getting personalized feed for user: {}, page: {}", userId, pageable.getPageNumber());

        // Cold start scenario
        if (preferences == null || preferences.isColdStart() || !preferences.hasPreferences()) {
            return getColdStartFeed(pageable);
        }

        // Step 1: Get candidate content
        List<ContentPost> candidates = getCandidateContent(preferences, userId);

        // Step 2: Apply diversity rules
        List<ContentPost> diversified = applyDiversityRules(candidates);

        // Step 3: Score and rank
        List<ScoredContent> scored = scoreAndRank(diversified, preferences);

        // Step 4: Convert to DTOs and paginate
        return toPaginatedDto(scored, pageable, userId);
    }

    @Override
    @Cacheable(value = "coldStartFeed", key = "#pageable.pageNumber", unless = "#result.isEmpty()")
    public Page<ContentDto> getColdStartFeed(Pageable pageable) {
        log.debug("Getting cold start feed, page: {}", pageable.getPageNumber());

        // Get popular content from last 7 days
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<ContentPost> candidates = contentPostRepository.findByPublishedAtAfter(since);

        // Apply diversity for cold start
        List<ContentPost> diversified = applyDiversityRules(candidates);

        // Score based on popularity and recency
        List<ScoredContent> scored = diversified.stream()
                .map(post -> {
                    double score = calculatePopularityScore(post);
                    String reason = "Popular in " + getPrimaryCategory(post);
                    return new ScoredContent(post, score, reason);
                })
                .sorted(Comparator.comparingDouble(ScoredContent::score).reversed())
                .toList();

        return toPaginatedDto(scored, pageable, null);
    }

    @Override
    public List<ContentDto> getSimilarContent(UUID contentId, int limit) {
        log.debug("Getting similar content for: {}, limit: {}", contentId, limit);

        Optional<ContentPost> sourceOpt = contentPostRepository.findById(contentId);
        if (sourceOpt.isEmpty()) {
            return List.of();
        }

        ContentPost source = sourceOpt.get();
        Map<String, Double> sourceCategories = getCategories(source);

        if (sourceCategories.isEmpty()) {
            // Fallback to trending
            return getTrendingContent(limit);
        }

        // Find content with overlapping categories
        List<ContentPost> allContent = contentPostRepository.findAll();

        List<ScoredContent> similar = allContent.stream()
                .filter(post -> !post.getId().equals(contentId))
                .map(post -> {
                    double overlap = calculateCategoryOverlap(sourceCategories, getCategories(post));
                    String reason = "Similar to content you viewed";
                    return new ScoredContent(post, overlap, reason);
                })
                .filter(sc -> sc.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredContent::score).reversed())
                .limit(limit)
                .toList();

        return similar.stream()
                .map(sc -> contentMapper.toDtoWithRecommendation(sc.post(), sc.score() / 10, sc.reason()))
                .toList();
    }

    @Override
    @Cacheable(value = "trending", key = "#limit", unless = "#result.isEmpty()")
    public List<ContentDto> getTrendingContent(int limit) {
        log.debug("Getting trending content, limit: {}", limit);

        LocalDateTime since = LocalDateTime.now().minusHours(24);

        // Get view counts for last 24 hours
        List<Object[]> trendingData = viewRepository.findTrendingContentIds(since, PageRequest.of(0, limit * 2));

        if (trendingData.isEmpty()) {
            // Fallback to recent content
            return contentPostRepository.findAll(
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "publishedAt"))
            ).stream()
                    .map(post -> contentMapper.toDtoWithRecommendation(post, 0.5, "Recent content"))
                    .toList();
        }

        // Get content posts by ID
        List<UUID> contentIds = trendingData.stream()
                .map(row -> (UUID) row[0])
                .limit(limit)
                .toList();

        Map<UUID, Long> viewCounts = trendingData.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a
                ));

        return contentIds.stream()
                .map(contentPostRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(post -> {
                    long views = viewCounts.getOrDefault(post.getId(), 0L);
                    double score = calculateTrendingScore(post, views);
                    String reason = "Trending with " + views + " views";
                    return contentMapper.toDtoWithRecommendation(post, score, reason);
                })
                .toList();
    }

    @Override
    public Page<ContentDto> getContentByCategories(List<String> categories, Pageable pageable) {
        log.debug("Getting content by categories: {}", categories);

        if (categories == null || categories.isEmpty()) {
            return Page.empty(pageable);
        }

        // Search for each category
        List<ContentPost> results = new ArrayList<>();
        for (String category : categories) {
            Page<ContentPost> categoryPosts = contentPostRepository.findByCategory(
                    category, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "publishedAt"))
            );
            results.addAll(categoryPosts.getContent());
        }

        // Remove duplicates and sort by published date
        List<ContentPost> unique = results.stream()
                .distinct()
                .sorted(Comparator.comparing(ContentPost::getPublishedAt).reversed())
                .toList();

        // Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), unique.size());

        if (start >= unique.size()) {
            return Page.empty(pageable);
        }

        List<ContentDto> dtos = unique.subList(start, end).stream()
                .map(post -> contentMapper.toDtoWithRecommendation(
                        post, 0.7, "Matches your category preferences"))
                .toList();

        return new PageImpl<>(dtos, pageable, unique.size());
    }

    /**
     * Gets candidate content based on user preferences.
     */
    private List<ContentPost> getCandidateContent(UserPreferences preferences, UUID userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<ContentPost> candidates = contentPostRepository.findByPublishedAtAfter(since);

        // Exclude bookmarked content
        if (userId != null) {
            Set<UUID> bookmarkedIds = bookmarkRepository.findByUserId(userId).stream()
                    .map(bookmark -> bookmark.getContentPost().getId())
                    .collect(Collectors.toSet());
            candidates = candidates.stream()
                    .filter(post -> !bookmarkedIds.contains(post.getId()))
                    .toList();
        }

        // Split into preferred (70%) and exploration (30%)
        if (preferences.hasPreferences()) {
            List<ContentPost> preferred = new ArrayList<>();
            List<ContentPost> exploration = new ArrayList<>();

            for (ContentPost post : candidates) {
                if (matchesPreferences(post, preferences)) {
                    preferred.add(post);
                } else {
                    exploration.add(post);
                }
            }

            // Combine with 70/30 ratio
            List<ContentPost> result = new ArrayList<>();
            int preferredCount = (int) (candidates.size() * 0.7);
            int explorationCount = candidates.size() - preferredCount;

            Collections.shuffle(preferred);
            Collections.shuffle(exploration);

            result.addAll(preferred.stream().limit(preferredCount).toList());
            result.addAll(exploration.stream().limit(explorationCount).toList());

            return result;
        }

        return candidates;
    }

    /**
     * Applies diversity rules to content list.
     */
    private List<ContentPost> applyDiversityRules(List<ContentPost> content) {
        if (content.isEmpty()) {
            return content;
        }

        List<ContentPost> result = new ArrayList<>();
        String lastCategory = null;
        int categoryContinuity = 0;
        int sinceLastMedia = 0;

        // Sort by published date first
        List<ContentPost> sorted = new ArrayList<>(content);
        sorted.sort(Comparator.comparing(ContentPost::getPublishedAt).reversed());

        for (ContentPost post : sorted) {
            String category = getPrimaryCategory(post);

            // Rule 1: No more than 3 consecutive same category
            if (category != null && category.equals(lastCategory)) {
                categoryContinuity++;
                if (categoryContinuity >= MAX_CONSECUTIVE_SAME_CATEGORY) {
                    continue; // Skip to ensure diversity
                }
            } else {
                categoryContinuity = 1;
                lastCategory = category;
            }

            // Rule 2: Insert media content periodically
            if (post.getHasMedia()) {
                sinceLastMedia = 0;
            } else {
                sinceLastMedia++;
                // If too many non-media posts, try to insert media
                if (sinceLastMedia > MEDIA_INSERT_INTERVAL) {
                    // Find next media post in remaining content
                    Optional<ContentPost> mediaPost = sorted.stream()
                            .filter(p -> !result.contains(p) && p.getHasMedia())
                            .findFirst();
                    if (mediaPost.isPresent()) {
                        result.add(mediaPost.get());
                        sinceLastMedia = 0;
                    }
                }
            }

            result.add(post);
        }

        return result;
    }

    /**
     * Scores and ranks content based on preferences.
     */
    private List<ScoredContent> scoreAndRank(List<ContentPost> content, UserPreferences preferences) {
        LocalDateTime now = LocalDateTime.now();
        Set<UUID> trendingIds = getTrendingContentIds();
        Random random = new Random(now.toLocalDate().toEpochDay()); // Deterministic for caching

        return content.stream()
                .map(post -> {
                    double score = 0.0;
                    List<String> reasons = new ArrayList<>();

                    // Category match score
                    Map<String, Double> postCategories = getCategories(post);
                    for (String category : postCategories.keySet()) {
                        if (preferences.prefersCategory(category)) {
                            score += CATEGORY_MATCH_SCORE * postCategories.get(category);
                            reasons.add("Matches " + category);
                        }
                    }

                    // Content type match
                    if (preferences.prefersContentType(post.getContentType().name())) {
                        score += CONTENT_TYPE_MATCH_SCORE;
                        reasons.add("Preferred format");
                    }

                    // Recency score (exponential decay)
                    long hoursOld = ChronoUnit.HOURS.between(post.getPublishedAt(), now);
                    double recencyScore = Math.exp(-hoursOld / 48.0) * 5; // Half-life of 48 hours
                    score += recencyScore;

                    // Trending bonus
                    if (trendingIds.contains(post.getId())) {
                        score += TRENDING_BONUS;
                        reasons.add("Trending");
                    }

                    // Random factor for variety
                    score += (random.nextDouble() - 0.5) * RANDOM_FACTOR_RANGE;

                    String reason = reasons.isEmpty() ? "Recommended for you" : String.join(", ", reasons);
                    return new ScoredContent(post, score, reason);
                })
                .sorted(Comparator.comparingDouble(ScoredContent::score).reversed())
                .toList();
    }

    /**
     * Converts scored content to paginated DTOs.
     */
    private Page<ContentDto> toPaginatedDto(List<ScoredContent> scored, Pageable pageable, UUID userId) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), scored.size());

        if (start >= scored.size()) {
            return Page.empty(pageable);
        }

        // Normalize scores to 0-1 range
        double maxScore = scored.stream().mapToDouble(ScoredContent::score).max().orElse(1.0);

        List<ContentDto> dtos = scored.subList(start, end).stream()
                .map(sc -> {
                    double normalizedScore = maxScore > 0 ? sc.score() / maxScore : 0.5;
                    ContentDto dto = contentMapper.toDtoWithRecommendation(
                            sc.post(), normalizedScore, sc.reason());

                    // Add engagement data
                    long viewCount = viewRepository.countByContentPostId(sc.post().getId());
                    long bookmarkCount = bookmarkRepository.countByContentPostId(sc.post().getId());
                    boolean isBookmarked = userId != null &&
                            bookmarkRepository.existsByUserIdAndContentPostId(userId, sc.post().getId());

                    dto.setViewCount(viewCount);
                    dto.setBookmarkCount(bookmarkCount);
                    dto.setIsBookmarked(isBookmarked);

                    return dto;
                })
                .toList();

        return new PageImpl<>(dtos, pageable, scored.size());
    }

    /**
     * Checks if content matches user preferences.
     */
    private boolean matchesPreferences(ContentPost post, UserPreferences preferences) {
        Map<String, Double> categories = getCategories(post);
        return categories.keySet().stream()
                .anyMatch(preferences::prefersCategory);
    }

    /**
     * Gets the primary category of a content post.
     */
    private String getPrimaryCategory(ContentPost post) {
        if (post.getMetadata() != null) {
            return post.getMetadata().getPrimaryCategory();
        }
        return null;
    }

    /**
     * Gets categories map from content post.
     */
    private Map<String, Double> getCategories(ContentPost post) {
        if (post.getMetadata() == null || post.getMetadata().getCategories() == null) {
            return Map.of();
        }
        return contentMapper.parseCategories(post.getMetadata().getCategories());
    }

    /**
     * Calculates category overlap score between two category maps.
     */
    private double calculateCategoryOverlap(Map<String, Double> source, Map<String, Double> target) {
        double overlap = 0.0;
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            if (target.containsKey(entry.getKey())) {
                overlap += entry.getValue() * target.get(entry.getKey());
            }
        }
        return overlap;
    }

    /**
     * Calculates popularity score for cold start.
     */
    private double calculatePopularityScore(ContentPost post) {
        long views = viewRepository.countByContentPostId(post.getId());
        long bookmarks = bookmarkRepository.countByContentPostId(post.getId());

        // Recency factor
        long hoursOld = ChronoUnit.HOURS.between(post.getPublishedAt(), LocalDateTime.now());
        double recencyFactor = Math.exp(-hoursOld / 72.0);

        return (views + bookmarks * 3) * recencyFactor;
    }

    /**
     * Calculates trending score.
     */
    private double calculateTrendingScore(ContentPost post, long views) {
        long hoursOld = Math.max(1, ChronoUnit.HOURS.between(post.getPublishedAt(), LocalDateTime.now()));
        return views * (1.0 / hoursOld);
    }

    /**
     * Gets IDs of trending content.
     */
    private Set<UUID> getTrendingContentIds() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return viewRepository.findTrendingContentIds(since, PageRequest.of(0, 50)).stream()
                .map(row -> (UUID) row[0])
                .collect(Collectors.toSet());
    }

    /**
     * Record for holding scored content.
     */
    private record ScoredContent(ContentPost post, double score, String reason) {}
}
