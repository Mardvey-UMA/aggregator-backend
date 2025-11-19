package com.contentaggregation.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for password strength requirements.
 *
 * <p>Password must:
 * <ul>
 *   <li>Be at least 8 characters long</li>
 *   <li>Contain at least one uppercase letter</li>
 *   <li>Contain at least one lowercase letter</li>
 *   <li>Contain at least one digit</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must be at least 8 characters and contain uppercase, lowercase, and digit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
