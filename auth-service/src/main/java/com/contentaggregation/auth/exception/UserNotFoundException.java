package com.contentaggregation.auth.exception;

import java.util.UUID;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String message) {
        super("USER_NOT_FOUND", message);
    }

    public static UserNotFoundException byId(UUID id) {
        return new UserNotFoundException("User not found with id: " + id);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found with username: " + username);
    }
}
