package com.contentaggregation.auth.repository;

import com.contentaggregation.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity database operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email address.
     *
     * @param email user's email
     * @return optional user
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by username.
     *
     * @param username user's username
     * @return optional user
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email address, ignoring case.
     *
     * @param email user's email
     * @return optional user
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Finds a user by username, ignoring case.
     *
     * @param username user's username
     * @return optional user
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email email to check
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     *
     * @param username username to check
     * @return true if user exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the given email, ignoring case.
     *
     * @param email email to check
     * @return true if user exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if a user exists with the given username, ignoring case.
     *
     * @param username username to check
     * @return true if user exists
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Updates the last login timestamp for a user.
     *
     * @param userId user ID
     * @param lastLoginAt timestamp to set
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    int updateLastLoginAt(@Param("userId") UUID userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * Verifies a user's email by setting emailVerified to true.
     *
     * @param userId user ID
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    int verifyEmail(@Param("userId") UUID userId);

    /**
     * Enables or disables a user account.
     *
     * @param userId user ID
     * @param enabled enabled status
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    int updateEnabled(@Param("userId") UUID userId, @Param("enabled") boolean enabled);

    /**
     * Updates the user's password hash.
     *
     * @param userId user ID
     * @param passwordHash new password hash
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    int updatePasswordHash(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);
}
