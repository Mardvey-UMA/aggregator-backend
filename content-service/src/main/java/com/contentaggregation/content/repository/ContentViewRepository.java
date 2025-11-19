package com.contentaggregation.content.repository;

import com.contentaggregation.content.entity.ContentView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ContentView entity operations.
 *
 * <p>Provides custom queries for view tracking and trending calculations.
 */
@Repository
public interface ContentViewRepository extends JpaRepository<ContentView, UUID> {

    /**
     * Counts views for a content post after a specific time.
     *
     * @param contentPostId the content post ID
     * @param viewedAt the time to count from
     * @return the view count
     */
    long countByContentPostIdAndViewedAtAfter(UUID contentPostId, LocalDateTime viewedAt);

    /**
     * Finds views for a content post.
     *
     * @param contentPostId the content post ID
     * @param pageable pagination information
     * @return page of views for the content
     */
    Page<ContentView> findByContentPostId(UUID contentPostId, Pageable pageable);

    /**
     * Finds views by user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of views by the user
     */
    Page<ContentView> findByUserId(UUID userId, Pageable pageable);

    /**
     * Calculates trending content based on view counts in a time period.
     *
     * @param since the start time for counting views
     * @param pageable pagination information
     * @return list of content post IDs with view counts
     */
    @Query("""
            SELECT cv.contentPost.id, COUNT(cv.id) as viewCount
            FROM ContentView cv
            WHERE cv.viewedAt >= :since
            GROUP BY cv.contentPost.id
            ORDER BY COUNT(cv.id) DESC
            """)
    List<Object[]> findTrendingContentIds(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Counts total views for a content post.
     *
     * @param contentPostId the content post ID
     * @return the total view count
     */
    long countByContentPostId(UUID contentPostId);

    /**
     * Deletes views older than a specific date.
     *
     * @param viewedAt the cutoff date
     * @return the number of deleted views
     */
    long deleteByViewedAtBefore(LocalDateTime viewedAt);

    /**
     * Finds views within a time range.
     *
     * @param start the start time
     * @param end the end time
     * @return list of views in the range
     */
    List<ContentView> findByViewedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Checks if a session has already viewed a content post.
     *
     * @param sessionId the session ID
     * @param contentPostId the content post ID
     * @return true if view exists for session
     */
    boolean existsBySessionIdAndContentPostId(UUID sessionId, UUID contentPostId);

    /**
     * Calculates hourly view counts for a content post.
     *
     * @param contentPostId the content post ID
     * @param since the start time
     * @return list of hourly counts
     */
    @Query("""
            SELECT FUNCTION('date_trunc', 'hour', cv.viewedAt) as hour, COUNT(cv.id)
            FROM ContentView cv
            WHERE cv.contentPost.id = :contentPostId
            AND cv.viewedAt >= :since
            GROUP BY FUNCTION('date_trunc', 'hour', cv.viewedAt)
            ORDER BY hour
            """)
    List<Object[]> findHourlyViewCounts(@Param("contentPostId") UUID contentPostId, @Param("since") LocalDateTime since);
}
