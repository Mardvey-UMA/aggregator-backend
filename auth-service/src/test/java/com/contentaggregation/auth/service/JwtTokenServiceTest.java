package com.contentaggregation.auth.service;

import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.service.impl.JwtTokenServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtTokenService.
 */
class JwtTokenServiceTest {

    private JwtTokenServiceImpl jwtTokenService;
    private User testUser;

    // Base64 encoded test secret
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbWluLTI1Ni1iaXRzLXJlcXVpcmVk";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenServiceImpl();
        ReflectionTestUtils.setField(jwtTokenService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenService, "issuer", "test-issuer");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("Should generate access token with correct claims")
    void shouldGenerateAccessTokenWithCorrectClaims() {
        String token = jwtTokenService.generateAccessToken(testUser);

        assertThat(token).isNotNull();

        Claims claims = jwtTokenService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo(testUser.getEmail());
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    @DisplayName("Should generate refresh token with correct expiry")
    void shouldGenerateRefreshTokenWithCorrectExpiry() {
        String token = jwtTokenService.generateRefreshToken(testUser);

        assertThat(token).isNotNull();

        Claims claims = jwtTokenService.extractAllClaims(token);
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Should validate valid access token")
    void shouldValidateValidAccessToken() {
        String token = jwtTokenService.generateAccessToken(testUser);

        boolean isValid = jwtTokenService.validateAccessToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate valid refresh token")
    void shouldValidateValidRefreshToken() {
        String token = jwtTokenService.generateRefreshToken(testUser);

        boolean isValid = jwtTokenService.validateRefreshToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject access token as refresh token")
    void shouldRejectAccessTokenAsRefreshToken() {
        String accessToken = jwtTokenService.generateAccessToken(testUser);

        boolean isValid = jwtTokenService.validateRefreshToken(accessToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        boolean isValid = jwtTokenService.validateAccessToken("invalid-token");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract userId from token correctly")
    void shouldExtractUserIdFromToken() {
        String token = jwtTokenService.generateAccessToken(testUser);

        UUID userId = jwtTokenService.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract email from token correctly")
    void shouldExtractEmailFromToken() {
        String token = jwtTokenService.generateAccessToken(testUser);

        String email = jwtTokenService.getEmailFromToken(token);

        assertThat(email).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void shouldDetectNonExpiredToken() {
        String token = jwtTokenService.generateAccessToken(testUser);

        boolean isExpired = jwtTokenService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectTokenWithInvalidSignature() {
        String token = jwtTokenService.generateAccessToken(testUser);
        // Tamper with the token
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        boolean isValid = jwtTokenService.validateAccessToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should get access token expiration")
    void shouldGetAccessTokenExpiration() {
        var expiration = jwtTokenService.getAccessTokenExpiration();

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(java.time.LocalDateTime.now());
    }
}
