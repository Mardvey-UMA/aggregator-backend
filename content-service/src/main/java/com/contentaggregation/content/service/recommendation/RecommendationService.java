package com.contentaggregation.content.service.recommendation;

import com.contentaggregation.content.dto.UserPreferences;
import com.contentaggregation.content.dto.response.ContentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for content recommendation.
 *
 * <p>Defines the contract for recommendation services (stub or ML-based).
 * Implementations should provide personalized, diverse, and relevant content
 * to users based on their preferences and behavior.
 */
public interface RecommendationService {

    /**
     * Gets a personalized content feed for a user.
     *
     * <p>The feed should:
     * <ul>
     *   <li>Prioritize content matching user preferences</li>
     *   <li>Apply diversity rules to avoid repetition</li>
     *   <li>Include exploration content for discovery</li>
     *   <li>Be deterministic for caching (same input = same output)</li>
     * </ul>
     *
     * @param userId user identifier (for exclusions and tracking)
     * @param preferences user preferences from profile or defaults
     * @param pageable pagination information
     * @return page of personalized content
     */
    Page<ContentDto> getPersonalizedFeed(UUID userId, UserPreferences preferences, Pageable pageable);

    /**
     * Gets content similar to a specific piece of content.
     *
     * <p>Similarity is determined by:
     * <ul>
     *   <li>Overlapping categories</li>
     *   <li>Similar entities</li>
     *   <li>Same content type</li>
     *   <li>Similar sentiment</li>
     * </ul>
     *
     * @param contentId the source content ID
     * @param limit maximum number of results
     * @return list of similar content
     */
    List<ContentDto> getSimilarContent(UUID contentId, int limit);

    /**
     * Gets trending content based on recent engagement.
     *
     * <p>Trending calculation considers:
     * <ul>
     *   <li>View count in last 24 hours</li>
     *   <li>Bookmark count</li>
     *   <li>Recency bonus</li>
     * </ul>
     *
     * @param limit maximum number of results
     * @return list of trending content
     */
    List<ContentDto> getTrendingContent(int limit);

    /**
     * Gets content for cold start users (no preferences).
     *
     * <p>Returns a balanced mix of:
     * <ul>
     *   <li>Popular content from all categories</li>
     *   <li>Recent trending content</li>
     *   <li>Diverse content types</li>
     * </ul>
     *
     * @param pageable pagination information
     * @return page of discovery content
     */
    Page<ContentDto> getColdStartFeed(Pageable pageable);

    /**
     * Gets content by specific categories.
     *
     * @param categories list of category names
     * @param pageable pagination information
     * @return page of content in the specified categories
     */
    Page<ContentDto> getContentByCategories(List<String> categories, Pageable pageable);
}
