package com.contentaggregation.auth.service.impl;

import com.contentaggregation.auth.dto.request.LoginRequest;
import com.contentaggregation.auth.dto.request.RegisterRequest;
import com.contentaggregation.auth.dto.response.AuthResponse;
import com.contentaggregation.auth.dto.response.UserDto;
import com.contentaggregation.auth.entity.RefreshToken;
import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.exception.InvalidCredentialsException;
import com.contentaggregation.auth.exception.InvalidTokenException;
import com.contentaggregation.auth.exception.UserAlreadyExistsException;
import com.contentaggregation.auth.exception.UserNotFoundException;
import com.contentaggregation.auth.repository.RefreshTokenRepository;
import com.contentaggregation.auth.repository.UserRepository;
import com.contentaggregation.auth.service.AuthenticationService;
import com.contentaggregation.auth.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Implementation of authentication service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.email());

        // Check if email already exists
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw UserAlreadyExistsException.byEmail(request.email());
        }

        // Check if username already exists
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw UserAlreadyExistsException.byUsername(request.username());
        }

        // Create new user
        User user = User.builder()
                .email(request.email().toLowerCase())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .enabled(true)
                .roles("USER")
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        // Find user by email
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Check if user has password (not OAuth-only)
        if (!user.hasPassword()) {
            throw new InvalidCredentialsException("Please login using your social account");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new InvalidCredentialsException("Account is disabled");
        }

        // Update last login
        user.updateLastLogin();
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Refreshing access token");

        // Validate the refresh token JWT
        if (!jwtTokenService.validateRefreshToken(refreshToken)) {
            throw InvalidTokenException.malformed();
        }

        // Find the token in database
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(InvalidTokenException::notFound);

        // Check if token is valid
        if (!storedToken.isValid()) {
            if (storedToken.getRevoked()) {
                throw InvalidTokenException.revoked();
            }
            throw InvalidTokenException.expired();
        }

        // Get user
        User user = storedToken.getUser();

        if (!user.getEnabled()) {
            throw new InvalidCredentialsException("Account is disabled");
        }

        // Revoke old token (mark as used and revoked)
        storedToken.markAsUsed();
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        log.info("Token refreshed for user: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.debug("Logging out user");

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(InvalidTokenException::notFound);

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        log.info("User logged out successfully: {}", storedToken.getUser().getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        // This is a stub - full implementation would decode verification token
        // and mark user's email as verified
        log.info("Email verification requested with token");

        // In full implementation:
        // 1. Decode the verification token
        // 2. Find user by token
        // 3. Mark email as verified
        // 4. Delete/invalidate the token

        throw new UnsupportedOperationException("Email verification not yet implemented");
    }

    /**
     * Generates authentication response with new tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        // Generate tokens
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        // Save refresh token to database
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        // Build response
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenService.getAccessTokenExpiration(),
                UserDto.fromEntity(user)
        );
    }
}
