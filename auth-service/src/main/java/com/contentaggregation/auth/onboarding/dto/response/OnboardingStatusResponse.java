package com.contentaggregation.auth.onboarding.dto.response;

import com.contentaggregation.auth.onboarding.enums.OnboardingStep;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OnboardingStatusResponse(
        UUID userId,
        OnboardingStep currentStep,
        boolean completed,
        boolean skipped,
        List<String> selectedCategories,
        List<String> selectedContentTypes,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}

