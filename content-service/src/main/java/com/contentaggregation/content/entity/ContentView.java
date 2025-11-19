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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a view event on a content post.
 *
 * <p>View events are used for tracking trending content and analytics.
 * Views can be anonymous (no userId) or authenticated.
 */
@Entity
@Table(name = "content_views", indexes = {
        @Index(name = "idx_views_content_post_id", columnList = "content_post_id"),
        @Index(name = "idx_views_viewed_at", columnList = "viewed_at"),
        @Index(name = "idx_views_user_id", columnList = "user_id"),
        @Index(name = "idx_views_content_viewed", columnList = "content_post_id, viewed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_post_id", nullable = false)
    private ContentPost contentPost;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
