package com.contentaggregation.auth.onboarding.service;

import com.contentaggregation.auth.onboarding.entity.CategoryOption;
import com.contentaggregation.auth.onboarding.exception.InvalidPreferenceException;
import com.contentaggregation.auth.onboarding.repository.CategoryOptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class PreferenceValidationService {

    private static final Set<String> VALID_CONTENT_TYPES = Set.of(
            "SHORT_POST",
            "MEDIUM_POST",
            "LONG_ARTICLE",
            "VIDEO_POST",
            "IMAGE_POST"
    );

    private final CategoryOptionRepository categoryRepo;
    private final int minCategories;
    private final int maxCategories;

    public PreferenceValidationService(
            CategoryOptionRepository categoryRepo,
            @Value("${onboarding.min-categories:3}") int minCategories,
            @Value("${onboarding.max-categories:5}") int maxCategories
    ) {
        this.categoryRepo = categoryRepo;
        this.minCategories = minCategories;
        this.maxCategories = maxCategories;
    }

    public ValidationResult validateCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return ValidationResult.invalid("Categories cannot be empty");
        }

        if (categories.size() < minCategories) {
            return ValidationResult.invalid("Select at least " + minCategories + " categories");
        }

        if (categories.size() > maxCategories) {
            return ValidationResult.invalid("Select at most " + maxCategories + " categories");
        }

        for (String categoryKey : categories) {
            if (categoryKey == null || categoryKey.isBlank()) {
                return ValidationResult.invalid("Category cannot be blank");
            }

            String normalized = categoryKey.trim().toLowerCase(Locale.ROOT);
            Optional<CategoryOption> option = categoryRepo.findByCategoryKey(normalized);
            if (option.isEmpty() || !option.get().isAvailable()) {
                return ValidationResult.invalid("Invalid category: " + categoryKey);
            }
        }

        return ValidationResult.valid();
    }

    public ValidationResult validateContentTypes(List<String> contentTypes) {
        if (contentTypes == null || contentTypes.isEmpty()) {
            return ValidationResult.invalid("Content types cannot be empty");
        }

        for (String type : contentTypes) {
            if (type == null || type.isBlank()) {
                return ValidationResult.invalid("Content type cannot be blank");
            }

            String normalized = type.trim().toUpperCase(Locale.ROOT);
            if (!VALID_CONTENT_TYPES.contains(normalized)) {
                return ValidationResult.invalid("Invalid content type: " + type);
            }
        }

        return ValidationResult.valid();
    }

    public record ValidationResult(boolean success, String errorMessage) {

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public void throwIfInvalid() {
            if (!success) {
                throw new InvalidPreferenceException(errorMessage);
            }
        }
    }
}

