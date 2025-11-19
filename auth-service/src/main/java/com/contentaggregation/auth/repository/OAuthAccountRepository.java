package com.contentaggregation.auth.repository;

import com.contentaggregation.auth.entity.OAuthAccount;
import com.contentaggregation.auth.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuthAccount entity database operations.
 */
@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    /**
     * Finds an OAuth account by provider and provider-specific ID.
     *
     * @param provider OAuth provider
     * @param providerId provider-specific user ID
     * @return optional OAuth account
     */
    Optional<OAuthAccount> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    /**
     * Finds all OAuth accounts for a user.
     *
     * @param userId user ID
     * @return list of OAuth accounts
     */
    List<OAuthAccount> findByUserId(UUID userId);

    /**
     * Finds all OAuth accounts for a user by specific provider.
     *
     * @param userId user ID
     * @param provider OAuth provider
     * @return list of OAuth accounts
     */
    List<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider);

    /**
     * Checks if an OAuth account exists for provider and provider ID.
     *
     * @param provider OAuth provider
     * @param providerId provider-specific user ID
     * @return true if account exists
     */
    boolean existsByProviderAndProviderId(OAuthProvider provider, String providerId);

    /**
     * Checks if a user has linked an OAuth account from a provider.
     *
     * @param userId user ID
     * @param provider OAuth provider
     * @return true if linked
     */
    boolean existsByUserIdAndProvider(UUID userId, OAuthProvider provider);

    /**
     * Updates OAuth tokens for an account.
     *
     * @param id account ID
     * @param accessToken new access token
     * @param refreshToken new refresh token
     * @param expiresAt token expiration time
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE OAuthAccount o SET o.accessToken = :accessToken, " +
           "o.refreshToken = :refreshToken, o.expiresAt = :expiresAt " +
           "WHERE o.id = :id")
    int updateTokens(@Param("id") UUID id,
                     @Param("accessToken") String accessToken,
                     @Param("refreshToken") String refreshToken,
                     @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * Deletes all OAuth accounts for a user.
     *
     * @param userId user ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Counts OAuth accounts linked to a user.
     *
     * @param userId user ID
     * @return number of linked accounts
     */
    long countByUserId(UUID userId);
}
