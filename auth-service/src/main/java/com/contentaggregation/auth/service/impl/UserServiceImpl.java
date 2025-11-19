package com.contentaggregation.auth.service.impl;

import com.contentaggregation.auth.dto.request.UpdateUserRequest;
import com.contentaggregation.auth.dto.response.UserDto;
import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.exception.UserAlreadyExistsException;
import com.contentaggregation.auth.exception.UserNotFoundException;
import com.contentaggregation.auth.repository.OAuthAccountRepository;
import com.contentaggregation.auth.repository.RefreshTokenRepository;
import com.contentaggregation.auth.repository.UserRepository;
import com.contentaggregation.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of user service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        // Update email if provided and different
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.email())) {
                throw UserAlreadyExistsException.byEmail(request.email());
            }
            user.setEmail(request.email().toLowerCase());
            user.setEmailVerified(false); // Require re-verification
            log.info("User email updated: {}", request.email());
        }

        // Update username if provided and different
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(request.username())) {
                throw UserAlreadyExistsException.byUsername(request.username());
            }
            user.setUsername(request.username());
            log.info("User username updated: {}", request.username());
        }

        user = userRepository.save(user);
        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        // Delete OAuth accounts
        oAuthAccountRepository.deleteByUserId(userId);

        // Delete user
        userRepository.delete(user);

        log.info("User deleted: {}", userId);
    }
}
