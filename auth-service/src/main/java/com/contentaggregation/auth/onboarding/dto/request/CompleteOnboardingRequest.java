package com.contentaggregation.auth.onboarding.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompleteOnboardingRequest(
        @NotEmpty
        @Size(min = 3, max = 5)
        List<String> selectedCategories,

        @NotEmpty
        List<String> selectedContentTypes
) {
}

