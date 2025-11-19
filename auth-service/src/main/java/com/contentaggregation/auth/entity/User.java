package com.contentaggregation.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing authenticated users in the system.
 *
 * <p>Stores user credentials, profile information, and authentication state.
 * Supports both email/password and OAuth2 authentication methods.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_last_login_at", columnList = "last_login_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "roles", nullable = false, length = 255)
    @Builder.Default
    private String roles = "USER";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Updates the last login timestamp to current time.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Checks if user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    /**
     * Gets user roles as a set.
     *
     * @return set of role names
     */
    public Set<String> getRolesAsSet() {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(roles.split(",")));
    }

    /**
     * Sets roles from a set of role names.
     *
     * @param roleSet set of role names
     */
    public void setRolesFromSet(Set<String> roleSet) {
        this.roles = String.join(",", roleSet);
    }

    /**
     * Adds a role to the user.
     *
     * @param role role name to add
     */
    public void addRole(String role) {
        Set<String> roleSet = getRolesAsSet();
        roleSet.add(role.toUpperCase());
        setRolesFromSet(roleSet);
    }

    /**
     * Removes a role from the user.
     *
     * @param role role name to remove
     */
    public void removeRole(String role) {
        Set<String> roleSet = getRolesAsSet();
        roleSet.remove(role.toUpperCase());
        setRolesFromSet(roleSet);
    }

    /**
     * Checks if user can authenticate with password.
     *
     * @return true if password hash is set
     */
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }
}
