package com.contentaggregation.content.repository;

import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.enums.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ContentPost entity operations.
 *
 * <p>Provides custom queries for content retrieval, filtering, and trending calculations.
 */
@Repository
public interface ContentPostRepository extends JpaRepository<ContentPost, UUID> {

    /**
     * Finds content post by external ID from source.
     *
     * @param externalId the external identifier from the content source
     * @return optional containing the content post if found
     */
    Optional<ContentPost> findByExternalId(String externalId);

    /**
     * Finds all content posts published after a specific date.
     *
     * @param publishedAt the date to filter by
     * @return list of content posts published after the date
     */
    List<ContentPost> findByPublishedAtAfter(LocalDateTime publishedAt);

    /**
     * Finds content posts by content type with pagination.
     *
     * @param contentTypes list of content types to include
     * @param pageable pagination information
     * @return page of content posts matching the types
     */
    Page<ContentPost> findByContentTypeIn(List<ContentType> contentTypes, Pageable pageable);

    /**
     * Finds content posts by source with pagination.
     *
     * @param source the content source
     * @param pageable pagination information
     * @return page of content posts from the source
     */
    Page<ContentPost> findBySource(ContentSource source, Pageable pageable);

    /**
     * Finds content posts containing a specific category in their metadata.
     *
     * @param category the category to search for
     * @param pageable pagination information
     * @return page of content posts with the category
     */
    @Query("""
            SELECT cp FROM ContentPost cp
            JOIN cp.metadata m
            WHERE m.categories LIKE CONCAT('%"', :category, '"%')
            ORDER BY cp.publishedAt DESC
            """)
    Page<ContentPost> findByCategory(@Param("category") String category, Pageable pageable);

    /**
     * Finds trending content based on view count in the last specified hours.
     *
     * @param since the datetime to count views from
     * @param pageable pagination information
     * @return page of content posts ordered by view count
     */
    @Query("""
            SELECT cp FROM ContentPost cp
            LEFT JOIN ContentView cv ON cv.contentPost = cp AND cv.viewedAt >= :since
            GROUP BY cp.id
            ORDER BY COUNT(cv.id) DESC, cp.publishedAt DESC
            """)
    Page<ContentPost> findTrending(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Counts views for a specific content post in the last 24 hours.
     *
     * @param contentId the content post ID
     * @param since the datetime to count from
     * @return the view count
     */
    @Query("""
            SELECT COUNT(cv) FROM ContentView cv
            WHERE cv.contentPost.id = :contentId
            AND cv.viewedAt >= :since
            """)
    long countViewsInPeriod(@Param("contentId") UUID contentId, @Param("since") LocalDateTime since);

    /**
     * Finds content posts with media.
     *
     * @param pageable pagination information
     * @return page of content posts that have media
     */
    Page<ContentPost> findByHasMediaTrue(Pageable pageable);

    /**
     * Finds content posts by source channel ID.
     *
     * @param sourceChannelId the channel ID from the source
     * @param pageable pagination information
     * @return page of content posts from the channel
     */
    Page<ContentPost> findBySourceChannelId(String sourceChannelId, Pageable pageable);

    /**
     * Searches content posts by title or content text.
     *
     * @param searchTerm the term to search for
     * @param pageable pagination information
     * @return page of content posts matching the search
     */
    @Query("""
            SELECT cp FROM ContentPost cp
            WHERE LOWER(cp.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(cp.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY cp.publishedAt DESC
            """)
    Page<ContentPost> searchByTitleOrContent(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds content posts published within a date range.
     *
     * @param start the start of the date range
     * @param end the end of the date range
     * @param pageable pagination information
     * @return page of content posts in the date range
     */
    Page<ContentPost> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Finds long-form content posts (content length > 2000).
     *
     * @param pageable pagination information
     * @return page of long-form content posts
     */
    @Query("SELECT cp FROM ContentPost cp WHERE cp.contentLength > 2000 ORDER BY cp.publishedAt DESC")
    Page<ContentPost> findLongFormContent(Pageable pageable);

    /**
     * Counts content posts by source.
     *
     * @param source the content source
     * @return the count of posts from the source
     */
    long countBySource(ContentSource source);

    /**
     * Finds recent content posts with their metadata eagerly loaded.
     *
     * @param pageable pagination information
     * @return page of content posts with metadata
     */
    @Query("""
            SELECT cp FROM ContentPost cp
            LEFT JOIN FETCH cp.metadata
            ORDER BY cp.publishedAt DESC
            """)
    Page<ContentPost> findAllWithMetadata(Pageable pageable);
}
