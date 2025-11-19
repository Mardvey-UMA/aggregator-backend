package com.contentaggregation.auth.onboarding.service;

import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.onboarding.client.MetricsServiceClient;
import com.contentaggregation.auth.onboarding.dto.request.CategorySelectionRequest;
import com.contentaggregation.auth.onboarding.entity.CategoryOption;
import com.contentaggregation.auth.onboarding.entity.OnboardingStatus;
import com.contentaggregation.auth.onboarding.exception.InvalidPreferenceException;
import com.contentaggregation.auth.onboarding.metrics.OnboardingMetrics;
import com.contentaggregation.auth.onboarding.repository.CategoryOptionRepository;
import com.contentaggregation.auth.onboarding.repository.OnboardingStatusRepository;
import com.contentaggregation.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private OnboardingStatusRepository onboardingStatusRepository;

    @Mock
    private CategoryOptionRepository categoryOptionRepository;

    @Mock
    private PreferenceValidationService validationService;

    @Mock
    private MetricsServiceClient metricsServiceClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnboardingMetrics onboardingMetrics;

    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(
                onboardingStatusRepository,
                categoryOptionRepository,
                validationService,
                metricsServiceClient,
                onboardingMetrics,
                userRepository
        );
        ReflectionTestUtils.setField(onboardingService, "onboardingVersion", "v1");

        lenient().when(onboardingStatusRepository.save(any(OnboardingStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldValidateCategoriesBeforeSaving() {
        UUID userId = UUID.randomUUID();
        when(validationService.validateCategories(anyList()))
                .thenReturn(new PreferenceValidationService.ValidationResult(false, "invalid"));

        CategorySelectionRequest request = new CategorySelectionRequest(List.of("technology", "science", "business"));

        assertThatThrownBy(() -> onboardingService.saveCategories(userId, request))
                .isInstanceOf(InvalidPreferenceException.class);
    }

    @Test
    void shouldApplyDefaultsWhenSkippingOnboarding() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("skip@example.com")
                .username("skip")
                .build();

        when(onboardingStatusRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryOptionRepository.findByEnabledTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(
                        buildCategory("technology"),
                        buildCategory("science")
                ));

        onboardingService.skipOnboarding(userId);

        ArgumentCaptor<OnboardingStatus> statusCaptor = ArgumentCaptor.forClass(OnboardingStatus.class);
        verify(onboardingStatusRepository, times(2)).save(statusCaptor.capture());

        OnboardingStatus savedStatus = statusCaptor.getAllValues().getLast();
        assertThat(savedStatus.getSkipped()).isTrue();
        assertThat(savedStatus.getSelectedCategories()).containsExactly("technology", "science");
        assertThat(savedStatus.getSelectedContentTypes())
                .containsExactly("SHORT_POST", "MEDIUM_POST", "LONG_ARTICLE", "VIDEO_POST", "IMAGE_POST");

        verify(onboardingMetrics).recordOnboardingStarted();
        verify(onboardingMetrics).recordOnboardingSkipped();
    }

    private CategoryOption buildCategory(String key) {
        CategoryOption option = new CategoryOption();
        option.setCategoryKey(key);
        option.setCategoryName(key);
        option.setEnabled(true);
        return option;
    }
}

