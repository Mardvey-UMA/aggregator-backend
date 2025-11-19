package com.contentaggregation.auth.onboarding.converter;

import com.contentaggregation.auth.onboarding.enums.OnboardingStep;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Attribute converter to persist {@link OnboardingStep} as lowercase strings
 * while keeping strong typing inside the domain model.
 */
@Converter(autoApply = false)
public class OnboardingStepConverter implements AttributeConverter<OnboardingStep, String> {

    @Override
    public String convertToDatabaseColumn(OnboardingStep attribute) {
        if (attribute == null) {
            return OnboardingStep.NOT_STARTED.name().toLowerCase();
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public OnboardingStep convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return OnboardingStep.NOT_STARTED;
        }
        return OnboardingStep.valueOf(dbData.trim().toUpperCase());
    }
}
