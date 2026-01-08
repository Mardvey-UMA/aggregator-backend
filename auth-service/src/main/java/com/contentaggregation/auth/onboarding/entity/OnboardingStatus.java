package com.contentaggregation.auth.onboarding.entity;

import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.onboarding.converter.OnboardingStepConverter;
import com.contentaggregation.auth.onboarding.enums.OnboardingStep;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks onboarding state, selections, and timestamps for a user.
 */
@Entity
@Table(
    name = "onboarding_status",
    indexes = {
        @Index(name = "idx_onboarding_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_onboarding_completed", columnList = "completed"),
        @Index(name = "idx_onboarding_step", columnList = "step")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_onboarding_status_user"))
    private User user;

    @Convert(converter = OnboardingStepConverter.class)
    @Column(name = "step", nullable = false, length = 50)
    @Builder.Default
    private OnboardingStep step = OnboardingStep.NOT_STARTED;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "skipped", nullable = false)
    @Builder.Default
    private Boolean skipped = false;

    @Type(JsonType.class)
    @Column(name = "selected_categories", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> selectedCategories = new ArrayList<>();

    @Type(JsonType.class)
    @Column(name = "selected_content_types", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> selectedContentTypes = new ArrayList<>();

    @Column(name = "onboarding_version", length = 10, nullable = false)
    @Builder.Default
    private String onboardingVersion = "v1";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Marks onboarding as started and ensures initial timestamps are recorded.
     */
    public void markAsStarted() {
        this.step = OnboardingStep.CATEGORY_SELECTION;
        this.completed = false;
        this.skipped = false;
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    /**
     * Marks onboarding as completed.
     */
    public void markAsCompleted() {
        this.completed = true;
        this.skipped = false;
        this.step = OnboardingStep.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Marks onboarding as skipped.
     */
    public void markAsSkipped() {
        this.completed = false;
        this.skipped = true;
        this.step = OnboardingStep.SKIPPED;
        this.completedAt = null;
    }

    /**
     * Updates the onboarding step while maintaining audit flags.
     *
     * @param nextStep step to transition to
     */
    public void updateStep(OnboardingStep nextStep) {
        this.step = Objects.requireNonNull(nextStep, "nextStep");
        if (nextStep != OnboardingStep.SKIPPED) {
            this.skipped = false;
        }
        if (nextStep == OnboardingStep.COMPLETED) {
            markAsCompleted();
        }
    }

    /**
     * Adds a category to the selection if not already added.
     *
     * @param categoryKey category identifier
     */
    public void addSelectedCategory(String categoryKey) {
        if (categoryKey == null || categoryKey.isBlank()) {
            return;
        }
        if (selectedCategories == null) {
            selectedCategories = new ArrayList<>();
        }
        String normalized = categoryKey.trim().toLowerCase();
        if (!selectedCategories.contains(normalized)) {
            selectedCategories.add(normalized);
        }
    }

    /**
     * Adds a content type preference to the selection if not already added.
     *
     * @param contentType content type identifier
     */
    public void addSelectedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        if (selectedContentTypes == null) {
            selectedContentTypes = new ArrayList<>();
        }
        String normalized = contentType.trim().toUpperCase();
        if (!selectedContentTypes.contains(normalized)) {
            selectedContentTypes.add(normalized);
        }
    }
}
