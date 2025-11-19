package com.contentaggregation.content.repository;

import com.contentaggregation.content.entity.ContentBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ContentBookmark entity operations.
 *
 * <p>Provides custom queries for bookmark management and retrieval.
 */
@Repository
public interface ContentBookmarkRepository extends JpaRepository<ContentBookmark, UUID> {

    /**
     * Checks if a user has bookmarked a specific content post.
     *
     * @param userId the user ID
     * @param contentPostId the content post ID
     * @return true if bookmark exists
     */
    boolean existsByUserIdAndContentPostId(UUID userId, UUID contentPostId);

    /**
     * Deletes a bookmark by user ID and content post ID.
     *
     * @param userId the user ID
     * @param contentPostId the content post ID
     */
    void deleteByUserIdAndContentPostId(UUID userId, UUID contentPostId);

    /**
     * Finds all bookmarks for a user ordered by creation date descending.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of bookmarks for the user
     */
    Page<ContentBookmark> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds all bookmarks for a user.
     *
     * @param userId the user ID
     * @return list of bookmarks for the user
     */
    List<ContentBookmark> findByUserId(UUID userId);

    /**
     * Counts bookmarks for a specific content post.
     *
     * @param contentPostId the content post ID
     * @return the bookmark count
     */
    long countByContentPostId(UUID contentPostId);

    /**
     * Finds the most bookmarked content posts.
     *
     * @param pageable pagination information
     * @return page of content post IDs ordered by bookmark count
     */
    @Query("""
            SELECT cb.contentPost.id FROM ContentBookmark cb
            GROUP BY cb.contentPost.id
            ORDER BY COUNT(cb.id) DESC
            """)
    Page<UUID> findMostBookmarkedContentIds(Pageable pageable);

    /**
     * Deletes all bookmarks for a user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Finds bookmarks with content posts eagerly loaded.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of bookmarks with content
     */
    @Query("""
            SELECT cb FROM ContentBookmark cb
            JOIN FETCH cb.contentPost
            WHERE cb.userId = :userId
            ORDER BY cb.createdAt DESC
            """)
    Page<ContentBookmark> findByUserIdWithContent(@Param("userId") UUID userId, Pageable pageable);
}
