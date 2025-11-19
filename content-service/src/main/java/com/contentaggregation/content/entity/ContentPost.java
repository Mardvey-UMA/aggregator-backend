package com.contentaggregation.content.entity;

import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.enums.ContentType;
import com.contentaggregation.content.enums.MediaType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a content post in the aggregation system.
 *
 * <p>Content posts are the primary data model for aggregated content from various sources.
 * Each post contains the content text, metadata about the source, and optional media attachments.
 */
@Entity
@Table(name = "content_posts", indexes = {
        @Index(name = "idx_content_posts_external_id", columnList = "externalId"),
        @Index(name = "idx_content_posts_source", columnList = "source"),
        @Index(name = "idx_content_posts_published_at", columnList = "publishedAt"),
        @Index(name = "idx_content_posts_content_type", columnList = "contentType"),
        @Index(name = "idx_content_posts_source_channel", columnList = "sourceChannelId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentSource source;

    @Column(name = "source_channel_id")
    private String sourceChannelId;

    @Column(name = "source_channel_name")
    private String sourceChannelName;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "content_length", nullable = false)
    private Integer contentLength;

    @Column(name = "has_media", nullable = false)
    @Builder.Default
    private Boolean hasMedia = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_urls", columnDefinition = "jsonb")
    private String mediaUrls;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "contentPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ContentMetadata metadata;

    /**
     * Determines if the content is considered long-form.
     *
     * @return true if content length exceeds 2000 characters
     */
    public boolean isLongForm() {
        return contentLength != null && contentLength > 2000;
    }

    /**
     * Sets the bidirectional relationship with ContentMetadata.
     *
     * @param metadata the metadata to associate with this post
     */
    public void setMetadata(ContentMetadata metadata) {
        this.metadata = metadata;
        if (metadata != null) {
            metadata.setContentPost(this);
        }
    }
}
