package com.contentaggregation.auth.repository;

import com.contentaggregation.auth.entity.RefreshToken;
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
 * Repository for RefreshToken entity database operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a refresh token by its token value.
     *
     * @param token token string
     * @return optional refresh token
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Finds all refresh tokens for a user.
     *
     * @param userId user ID
     * @return list of refresh tokens
     */
    List<RefreshToken> findByUserId(UUID userId);

    /**
     * Finds all valid (not expired, not revoked) refresh tokens for a user.
     *
     * @param userId user ID
     * @return list of valid refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") UUID userId);

    /**
     * Revokes all refresh tokens for a user.
     *
     * @param userId user ID
     * @return number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    int revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * Revokes all refresh tokens for a user except the specified token.
     *
     * @param userId user ID
     * @param tokenToKeep token to exclude from revocation
     * @return number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
           "WHERE rt.user.id = :userId AND rt.token != :tokenToKeep")
    int revokeAllByUserIdExcept(@Param("userId") UUID userId, @Param("tokenToKeep") String tokenToKeep);

    /**
     * Deletes expired and revoked tokens older than specified date.
     *
     * @param before delete tokens created before this date
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE " +
           "(rt.revoked = true OR rt.expiresAt < CURRENT_TIMESTAMP) " +
           "AND rt.createdAt < :before")
    int deleteExpiredAndRevokedTokens(@Param("before") LocalDateTime before);

    /**
     * Counts valid (not expired, not revoked) tokens for a user.
     *
     * @param userId user ID
     * @return count of valid tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countValidTokensByUserId(@Param("userId") UUID userId);

    /**
     * Checks if a token exists and is valid.
     *
     * @param token token string
     * @return true if token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
           "FROM RefreshToken rt WHERE rt.token = :token " +
           "AND rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    boolean existsValidToken(@Param("token") String token);

    /**
     * Marks a token as used by updating the used_at timestamp.
     *
     * @param token token string
     * @param usedAt timestamp when token was used
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.usedAt = :usedAt WHERE rt.token = :token")
    int markAsUsed(@Param("token") String token, @Param("usedAt") LocalDateTime usedAt);

    /**
     * Finds tokens by device info for a user.
     *
     * @param userId user ID
     * @param deviceInfo device information
     * @return list of refresh tokens
     */
    List<RefreshToken> findByUserIdAndDeviceInfo(UUID userId, String deviceInfo);

    /**
     * Counts all tokens (including expired/revoked) for a user.
     *
     * @param userId user ID
     * @return total count of tokens
     */
    long countByUserId(UUID userId);
}
