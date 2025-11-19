package com.contentaggregation.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's bookmark on a content post.
 *
 * <p>Bookmarks allow users to save content for later viewing. Each user can bookmark
 * a specific content post only once.
 */
@Entity
@Table(name = "content_bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bookmarks_user_content",
                        columnNames = {"user_id", "content_post_id"})
        },
        indexes = {
                @Index(name = "idx_bookmarks_user_id", columnList = "user_id"),
                @Index(name = "idx_bookmarks_content_post_id", columnList = "content_post_id"),
                @Index(name = "idx_bookmarks_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_post_id", nullable = false)
    private ContentPost contentPost;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
