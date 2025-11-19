package com.contentaggregation.auth.exception;

/**
 * Exception thrown when attempting to register a user that already exists.
 */
public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String field, String value) {
        super("USER_ALREADY_EXISTS",
              String.format("User with %s '%s' already exists", field, value));
    }

    public static UserAlreadyExistsException byEmail(String email) {
        return new UserAlreadyExistsException("email", email);
    }

    public static UserAlreadyExistsException byUsername(String username) {
        return new UserAlreadyExistsException("username", username);
    }
}
