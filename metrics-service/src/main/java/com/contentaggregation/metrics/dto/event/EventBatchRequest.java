package com.contentaggregation.metrics.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Batch of heterogeneous interaction events sent from the frontend SDK")
public record EventBatchRequest(
    @NotNull
    @Schema(description = "Batch identifier used for deduplication", example = "c5e00449-c323-4388-8ab8-9484438bcdc4")
    UUID batchId,

    @NotEmpty
    @Valid
    @Schema(description = "Ordered list of events contained in the batch")
    List<UserEventPayload> events,

    @NotNull
    @Schema(description = "Creation timestamp of the batch", example = "2024-05-16T22:11:03.000Z")
    Instant timestamp
) { }

