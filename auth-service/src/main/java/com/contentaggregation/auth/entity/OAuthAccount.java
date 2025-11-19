package com.contentaggregation.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OAuth account entity representing external OAuth2 provider accounts linked to users.
 *
 * <p>Stores OAuth2 tokens and provider-specific user information.
 * A user can have multiple OAuth accounts from different providers.
 */
@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_oauth_accounts_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        )
    },
    indexes = {
        @Index(name = "idx_oauth_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_oauth_accounts_provider", columnList = "provider")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_oauth_accounts_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "provider_username", length = 255)
    private String providerUsername;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Checks if the OAuth access token is expired.
     *
     * @return true if token is expired or expiration time is not set
     */
    public boolean isTokenExpired() {
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if the OAuth account has a valid refresh token.
     *
     * @return true if refresh token is available
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    /**
     * Updates the OAuth tokens and expiration time.
     *
     * @param accessToken new access token
     * @param refreshToken new refresh token (can be null)
     * @param expiresAt token expiration time
     */
    public void updateTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
        this.expiresAt = expiresAt;
    }
}
