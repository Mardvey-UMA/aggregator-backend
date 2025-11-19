package com.contentaggregation.auth.onboarding.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CategorySelectionRequest(
        @NotEmpty(message = "At least one category must be selected")
        @Size(min = 3, max = 5, message = "Select between 3 and 5 categories")
        List<String> selectedCategories
) {
}

