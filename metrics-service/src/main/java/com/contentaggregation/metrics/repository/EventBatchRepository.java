package com.contentaggregation.metrics.repository;

import com.contentaggregation.metrics.entity.EventBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventBatchRepository extends JpaRepository<EventBatch, UUID> {

    boolean existsByBatchId(UUID batchId);
}

