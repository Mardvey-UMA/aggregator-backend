package com.contentaggregation.auth.service.impl;

import com.contentaggregation.auth.dto.response.AuthResponse;
import com.contentaggregation.auth.dto.response.UserDto;
import com.contentaggregation.auth.dto.response.VKUserInfo;
import com.contentaggregation.auth.entity.OAuthAccount;
import com.contentaggregation.auth.entity.OAuthProvider;
import com.contentaggregation.auth.entity.RefreshToken;
import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.exception.OAuth2AuthenticationException;
import com.contentaggregation.auth.repository.OAuthAccountRepository;
import com.contentaggregation.auth.repository.RefreshTokenRepository;
import com.contentaggregation.auth.repository.UserRepository;
import com.contentaggregation.auth.service.JwtTokenService;
import com.contentaggregation.auth.service.OAuth2Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Implementation of OAuth2 service for VK authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.vk.client-id}")
    private String vkClientId;

    @Value("${spring.security.oauth2.client.registration.vk.client-secret}")
    private String vkClientSecret;

    @Value("${spring.security.oauth2.client.provider.vk.authorization-uri}")
    private String vkAuthorizationUri;

    @Value("${spring.security.oauth2.client.provider.vk.token-uri}")
    private String vkTokenUri;

    @Value("${spring.security.oauth2.client.provider.vk.user-info-uri}")
    private String vkUserInfoUri;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Override
    public String getVKAuthorizationUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromHttpUrl(vkAuthorizationUri)
                .queryParam("client_id", vkClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "email")
                .queryParam("state", state)
                .queryParam("v", "5.131")
                .build()
                .toUriString();
    }

    @Override
    @Transactional
    public AuthResponse authenticateWithVK(String code, String redirectUri) {
        log.info("Authenticating with VK OAuth2");

        // Exchange code for access token
        String tokenUrl = UriComponentsBuilder.fromHttpUrl(vkTokenUri)
                .queryParam("client_id", vkClientId)
                .queryParam("client_secret", vkClientSecret)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();

        ResponseEntity<String> tokenResponse;
        try {
            tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
        } catch (Exception ex) {
            log.error("Failed to exchange code for token: {}", ex.getMessage());
            throw OAuth2AuthenticationException.tokenExchangeFailed();
        }

        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
            throw OAuth2AuthenticationException.tokenExchangeFailed();
        }

        // Parse token response
        JsonNode tokenJson;
        String accessToken;
        String email;
        Long vkUserId;

        try {
            tokenJson = objectMapper.readTree(tokenResponse.getBody());
            accessToken = tokenJson.get("access_token").asText();
            vkUserId = tokenJson.get("user_id").asLong();
            email = tokenJson.has("email") ? tokenJson.get("email").asText() : null;
        } catch (Exception ex) {
            log.error("Failed to parse token response: {}", ex.getMessage());
            throw OAuth2AuthenticationException.tokenExchangeFailed();
        }

        // Fetch user info from VK API
        String userInfoUrl = UriComponentsBuilder.fromHttpUrl(vkUserInfoUri)
                .queryParam("access_token", accessToken)
                .build()
                .toUriString();

        ResponseEntity<String> userInfoResponse;
        try {
            userInfoResponse = restTemplate.getForEntity(userInfoUrl, String.class);
        } catch (Exception ex) {
            log.error("Failed to fetch user info: {}", ex.getMessage());
            throw OAuth2AuthenticationException.userInfoFailed();
        }

        VKUserInfo vkUserInfo;
        try {
            JsonNode userInfoJson = objectMapper.readTree(userInfoResponse.getBody());
            JsonNode userNode = userInfoJson.get("response").get(0);

            vkUserInfo = new VKUserInfo(
                    vkUserId,
                    email,
                    userNode.has("first_name") ? userNode.get("first_name").asText() : null,
                    userNode.has("last_name") ? userNode.get("last_name").asText() : null,
                    userNode.has("screen_name") ? userNode.get("screen_name").asText() : null,
                    userNode.has("photo_200") ? userNode.get("photo_200").asText() : null
            );
        } catch (Exception ex) {
            log.error("Failed to parse user info: {}", ex.getMessage());
            throw OAuth2AuthenticationException.userInfoFailed();
        }

        // Get or create user
        User user = getOrCreateUserFromVK(vkUserInfo, accessToken);

        // Update last login
        user.updateLastLogin();
        userRepository.save(user);

        log.info("VK OAuth2 authentication successful for user: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public User getOrCreateUserFromVK(VKUserInfo vkUserInfo, String accessToken) {
        String providerId = String.valueOf(vkUserInfo.id());

        // Check if OAuth account exists
        Optional<OAuthAccount> existingOAuth = oAuthAccountRepository
                .findByProviderAndProviderId(OAuthProvider.VK, providerId);

        if (existingOAuth.isPresent()) {
            // Update tokens and return existing user
            OAuthAccount oAuthAccount = existingOAuth.get();
            oAuthAccount.setAccessToken(accessToken);
            oAuthAccountRepository.save(oAuthAccount);
            return oAuthAccount.getUser();
        }

        // Check if user exists with same email
        User user;
        if (vkUserInfo.email() != null && !vkUserInfo.email().isEmpty()) {
            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(vkUserInfo.email());

            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // Create new user
                user = createUserFromVK(vkUserInfo);
            }
        } else {
            // No email from VK - create user with generated email
            String generatedEmail = "vk_" + vkUserInfo.id() + "@vk.oauth";
            user = User.builder()
                    .email(generatedEmail)
                    .username(vkUserInfo.getUsername())
                    .emailVerified(false)
                    .enabled(true)
                    .roles("USER")
                    .build();
            user = userRepository.save(user);
        }

        // Create OAuth account
        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .user(user)
                .provider(OAuthProvider.VK)
                .providerId(providerId)
                .providerUsername(vkUserInfo.getUsername())
                .providerEmail(vkUserInfo.email())
                .accessToken(accessToken)
                .build();

        oAuthAccountRepository.save(oAuthAccount);

        return user;
    }

    private User createUserFromVK(VKUserInfo vkUserInfo) {
        String username = generateUniqueUsername(vkUserInfo.getUsername());

        User user = User.builder()
                .email(vkUserInfo.email().toLowerCase())
                .username(username)
                .emailVerified(true) // VK email is verified by VK
                .enabled(true)
                .roles("USER")
                .build();

        return userRepository.save(user);
    }

    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsernameIgnoreCase(username)) {
            username = baseUsername + counter++;
        }

        return username;
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenService.getAccessTokenExpiration(),
                UserDto.fromEntity(user)
        );
    }
}
