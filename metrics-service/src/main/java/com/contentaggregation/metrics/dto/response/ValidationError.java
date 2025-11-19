package com.contentaggregation.metrics.dto.response;

public record ValidationError(
    String eventId,
    String field,
    String message
) { }

