package com.contentaggregation.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for password strength requirements.
 *
 * <p>Validates that password:
 * <ul>
 *   <li>Is at least 8 characters long</li>
 *   <li>Contains at least one uppercase letter</li>
 *   <li>Contains at least one lowercase letter</li>
 *   <li>Contains at least one digit</li>
 * </ul>
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (password.length() < MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                "Password must be at least " + MIN_LENGTH + " characters long"
            ).addConstraintViolation();
            valid = false;
        }

        if (!password.matches(UPPERCASE_PATTERN)) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one uppercase letter"
            ).addConstraintViolation();
            valid = false;
        }

        if (!password.matches(LOWERCASE_PATTERN)) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one lowercase letter"
            ).addConstraintViolation();
            valid = false;
        }

        if (!password.matches(DIGIT_PATTERN)) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one digit"
            ).addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
