package com.contentaggregation.auth.onboarding.repository;

import com.contentaggregation.auth.onboarding.entity.CategoryOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryOptionRepository extends JpaRepository<CategoryOption, UUID> {

    List<CategoryOption> findByEnabledTrueOrderByDisplayOrderAsc();

    Optional<CategoryOption> findByCategoryKey(String key);

    boolean existsByCategoryKey(String key);
}
