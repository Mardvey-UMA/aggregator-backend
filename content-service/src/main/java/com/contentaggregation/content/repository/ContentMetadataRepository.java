package com.contentaggregation.content.repository;

import com.contentaggregation.content.entity.ContentMetadata;
import com.contentaggregation.content.enums.Sentiment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ContentMetadata entity operations.
 *
 * <p>Provides custom queries for metadata retrieval and filtering.
 */
@Repository
public interface ContentMetadataRepository extends JpaRepository<ContentMetadata, UUID> {

    /**
     * Finds metadata by content post ID.
     *
     * @param contentPostId the content post ID
     * @return optional containing the metadata if found
     */
    Optional<ContentMetadata> findByContentPostId(UUID contentPostId);

    /**
     * Finds metadata containing specific keywords.
     *
     * @param keyword the keyword to search for
     * @param pageable pagination information
     * @return page of metadata containing the keyword
     */
    @Query("""
            SELECT m FROM ContentMetadata m
            WHERE m.keywords LIKE CONCAT('%"', :keyword, '"%')
            ORDER BY m.createdAt DESC
            """)
    Page<ContentMetadata> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Finds metadata by sentiment type.
     *
     * @param sentiment the sentiment type
     * @param pageable pagination information
     * @return page of metadata with the sentiment
     */
    Page<ContentMetadata> findBySentiment(Sentiment sentiment, Pageable pageable);

    /**
     * Finds metadata by language.
     *
     * @param language the language code
     * @return list of metadata in the language
     */
    List<ContentMetadata> findByLanguage(String language);

    /**
     * Finds metadata with high complexity scores.
     *
     * @param minScore minimum complexity score
     * @param pageable pagination information
     * @return page of metadata above the complexity threshold
     */
    Page<ContentMetadata> findByComplexityScoreGreaterThan(Float minScore, Pageable pageable);

    /**
     * Finds metadata with positive sentiment and high score.
     *
     * @param minScore minimum sentiment score
     * @return list of metadata with positive sentiment above threshold
     */
    @Query("""
            SELECT m FROM ContentMetadata m
            WHERE m.sentiment = 'POSITIVE'
            AND m.sentimentScore >= :minScore
            ORDER BY m.sentimentScore DESC
            """)
    List<ContentMetadata> findPositiveSentimentAboveScore(@Param("minScore") Float minScore);

    /**
     * Counts metadata by language.
     *
     * @param language the language code
     * @return the count of metadata in the language
     */
    long countByLanguage(String language);

    /**
     * Deletes metadata by content post ID.
     *
     * @param contentPostId the content post ID
     */
    void deleteByContentPostId(UUID contentPostId);
}
