package com.contentaggregation.auth.onboarding.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ContentTypeSelectionRequest(
        @NotEmpty(message = "At least one content type must be selected")
        List<String> selectedContentTypes
) {
}

