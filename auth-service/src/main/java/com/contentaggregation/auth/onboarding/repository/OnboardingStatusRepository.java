package com.contentaggregation.auth.onboarding.repository;

import com.contentaggregation.auth.onboarding.entity.OnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingStatusRepository extends JpaRepository<OnboardingStatus, UUID> {

    Optional<OnboardingStatus> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("SELECT COUNT(os) FROM OnboardingStatus os WHERE os.completed = :completed")
    long countByCompletedStatus(@Param("completed") boolean completed);

    @Query("SELECT COUNT(os) FROM OnboardingStatus os WHERE os.skipped = :skipped")
    long countBySkippedStatus(@Param("skipped") boolean skipped);

    @Query("""
            SELECT os FROM OnboardingStatus os
            WHERE os.completed = TRUE
              AND os.completedAt BETWEEN :start AND :end
            ORDER BY os.completedAt ASC
            """)
    List<OnboardingStatus> findCompletedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
