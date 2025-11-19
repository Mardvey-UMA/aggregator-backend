package com.contentaggregation.metrics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_batches")
@EntityListeners(AuditingEntityListener.class)
public class EventBatch {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private UUID batchId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Integer eventCount;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Builder.Default
    private Boolean processed = Boolean.FALSE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

