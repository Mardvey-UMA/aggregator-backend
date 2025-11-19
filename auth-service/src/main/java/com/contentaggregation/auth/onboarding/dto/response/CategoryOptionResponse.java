package com.contentaggregation.auth.onboarding.dto.response;

public record CategoryOptionResponse(
        String key,
        String name,
        String description,
        String iconUrl,
        int displayOrder
) {
}

