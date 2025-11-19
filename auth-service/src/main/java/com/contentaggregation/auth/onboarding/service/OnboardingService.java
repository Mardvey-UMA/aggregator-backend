package com.contentaggregation.auth.onboarding.service;

import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.exception.UserNotFoundException;
import com.contentaggregation.auth.onboarding.client.MetricsServiceClient;
import com.contentaggregation.auth.onboarding.dto.request.CategorySelectionRequest;
import com.contentaggregation.auth.onboarding.dto.request.CompleteOnboardingRequest;
import com.contentaggregation.auth.onboarding.dto.request.ContentTypeSelectionRequest;
import com.contentaggregation.auth.onboarding.dto.response.CategoryOptionResponse;
import com.contentaggregation.auth.onboarding.dto.response.OnboardingStatusResponse;
import com.contentaggregation.auth.onboarding.entity.CategoryOption;
import com.contentaggregation.auth.onboarding.entity.OnboardingStatus;
import com.contentaggregation.auth.onboarding.enums.OnboardingStep;
import com.contentaggregation.auth.onboarding.exception.OnboardingNotFoundException;
import com.contentaggregation.auth.onboarding.metrics.OnboardingMetrics;
import com.contentaggregation.auth.onboarding.repository.CategoryOptionRepository;
import com.contentaggregation.auth.onboarding.repository.OnboardingStatusRepository;
import com.contentaggregation.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingService {

    private static final List<String> DEFAULT_CONTENT_TYPES = List.of(
            "SHORT_POST",
            "MEDIUM_POST",
            "LONG_ARTICLE",
            "VIDEO_POST",
            "IMAGE_POST"
    );

    private final OnboardingStatusRepository onboardingRepo;
    private final CategoryOptionRepository categoryRepo;
    private final PreferenceValidationService validationService;
    private final MetricsServiceClient metricsClient;
    private final OnboardingMetrics onboardingMetrics;
    private final UserRepository userRepository;

    @Value("${onboarding.version:v1}")
    private String onboardingVersion;

    public OnboardingStatusResponse getOrCreateStatus(UUID userId) {
        OnboardingStatus status = onboardingRepo.findByUserId(userId)
                .orElseGet(() -> createInitialStatus(userId));
        return toResponse(status);
    }

    @Transactional(readOnly = true)
    public List<CategoryOptionResponse> getAvailableCategories() {
        return categoryRepo.findByEnabledTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public OnboardingStatusResponse saveCategories(UUID userId, CategorySelectionRequest request) {
        validationService.validateCategories(request.selectedCategories()).throwIfInvalid();

        OnboardingStatus status = onboardingRepo.findByUserId(userId)
                .orElseGet(() -> createInitialStatus(userId));

        ensureOnboardingStarted(status);

        List<String> normalized = normalizeCategories(request.selectedCategories());
        status.setSelectedCategories(new ArrayList<>(normalized));
        status.setOnboardingVersion(onboardingVersion);
        status.updateStep(OnboardingStep.CONTENT_TYPE_SELECTION);

        onboardingRepo.save(status);
        normalized.forEach(onboardingMetrics::recordCategorySelected);
        log.info("User {} updated onboarding categories {}", userId, normalized);
        return toResponse(status);
    }

    public OnboardingStatusResponse saveContentTypes(UUID userId, ContentTypeSelectionRequest request) {
        validationService.validateContentTypes(request.selectedContentTypes()).throwIfInvalid();

        OnboardingStatus status = onboardingRepo.findByUserId(userId)
                .orElseThrow(() -> new OnboardingNotFoundException(userId));

        List<String> normalized = normalizeContentTypes(request.selectedContentTypes());
        status.setSelectedContentTypes(new ArrayList<>(normalized));
        status.setOnboardingVersion(onboardingVersion);
        status.updateStep(OnboardingStep.CONTENT_TYPE_SELECTION);

        onboardingRepo.save(status);
        log.info("User {} updated onboarding content types {}", userId, normalized);
        return toResponse(status);
    }

    public OnboardingStatusResponse completeOnboarding(UUID userId, CompleteOnboardingRequest request) {
        validationService.validateCategories(request.selectedCategories()).throwIfInvalid();
        validationService.validateContentTypes(request.selectedContentTypes()).throwIfInvalid();

        OnboardingStatus status = onboardingRepo.findByUserId(userId)
                .orElseGet(() -> createInitialStatus(userId));

        ensureOnboardingStarted(status);

        List<String> categories = normalizeCategories(request.selectedCategories());
        List<String> contentTypes = normalizeContentTypes(request.selectedContentTypes());

        status.setSelectedCategories(new ArrayList<>(categories));
        status.setSelectedContentTypes(new ArrayList<>(contentTypes));
        status.setOnboardingVersion(onboardingVersion);
        status.markAsCompleted();

        onboardingRepo.save(status);

        Duration duration = Duration.ZERO;
        if (status.getStartedAt() != null && status.getCompletedAt() != null) {
            duration = Duration.between(status.getStartedAt(), status.getCompletedAt());
        }

        categories.forEach(onboardingMetrics::recordCategorySelected);
        onboardingMetrics.recordOnboardingCompleted(duration.isNegative() ? Duration.ZERO : duration);
        metricsClient.initializeUserProfile(userId, categories, contentTypes);
        log.info("User {} completed onboarding", userId);
        return toResponse(status);
    }

    public OnboardingStatusResponse skipOnboarding(UUID userId) {
        OnboardingStatus status = onboardingRepo.findByUserId(userId)
                .orElseGet(() -> createInitialStatus(userId));

        ensureOnboardingStarted(status);
        List<String> allCategories = categoryRepo.findByEnabledTrueOrderByDisplayOrderAsc()
                .stream()
                .map(CategoryOption::getCategoryKey)
                .map(key -> key.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));

        ensureOnboardingStarted(status);
        status.markAsSkipped();
        status.setSelectedCategories(new ArrayList<>(allCategories));
        status.setSelectedContentTypes(new ArrayList<>(DEFAULT_CONTENT_TYPES));
        status.setOnboardingVersion(onboardingVersion);

        onboardingRepo.save(status);

        onboardingMetrics.recordOnboardingSkipped();
        metricsClient.initializeUserProfile(userId, allCategories, DEFAULT_CONTENT_TYPES);
        log.info("User {} skipped onboarding; defaults applied", userId);
        return toResponse(status);
    }

    public OnboardingStatusResponse resetOnboarding(UUID userId) {
        OnboardingStatus status = onboardingRepo.findByUserId(userId)
                .orElseThrow(() -> new OnboardingNotFoundException(userId));

        status.setStep(OnboardingStep.NOT_STARTED);
        status.setCompleted(false);
        status.setSkipped(false);
        status.setSelectedCategories(new ArrayList<>());
        status.setSelectedContentTypes(new ArrayList<>());
        status.setStartedAt(null);
        status.setCompletedAt(null);
        status.setOnboardingVersion(onboardingVersion);

        onboardingRepo.save(status);
        log.info("User {} reset onboarding progress", userId);
        return toResponse(status);
    }

    private OnboardingStatus createInitialStatus(UUID userId) {
        User user = loadUser(userId);
        OnboardingStatus status = OnboardingStatus.builder()
                .user(user)
                .onboardingVersion(onboardingVersion)
                .build();
        return onboardingRepo.save(status);
    }

    private void ensureOnboardingStarted(OnboardingStatus status) {
        if (status.getStartedAt() == null) {
            status.markAsStarted();
            onboardingMetrics.recordOnboardingStarted();
        }
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));
    }

    private List<String> normalizeCategories(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private List<String> normalizeContentTypes(List<String> types) {
        if (types == null) {
            return List.of();
        }
        return types.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private OnboardingStatusResponse toResponse(OnboardingStatus status) {
        List<String> categories = safeList(status.getSelectedCategories());
        List<String> contentTypes = safeList(status.getSelectedContentTypes());
        UUID userId = status.getUser() != null ? status.getUser().getId() : null;
        return new OnboardingStatusResponse(
                userId,
                status.getStep(),
                Boolean.TRUE.equals(status.getCompleted()),
                Boolean.TRUE.equals(status.getSkipped()),
                categories,
                contentTypes,
                status.getStartedAt(),
                status.getCompletedAt()
        );
    }

    private List<String> safeList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private CategoryOptionResponse toCategoryResponse(CategoryOption option) {
        return new CategoryOptionResponse(
                option.getCategoryKey(),
                option.getCategoryName(),
                option.getDescription(),
                option.getIconUrl(),
                option.getDisplayOrder() != null ? option.getDisplayOrder() : 0
        );
    }
}

