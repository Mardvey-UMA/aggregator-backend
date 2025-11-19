package com.contentaggregation.auth.onboarding.exception;

import java.util.UUID;

public class OnboardingNotFoundException extends RuntimeException {

    public OnboardingNotFoundException(UUID userId) {
        super("Onboarding status not found for user: " + userId);
    }
}

