package com.contentaggregation.metrics.dto.response;

import java.util.List;

public record EventIngestionResponse(
    String status,
    int acceptedEvents,
    int rejectedEvents,
    List<ValidationError> errors
) {

    public static EventIngestionResponse success(int accepted, int rejected, List<ValidationError> errors) {
        return new EventIngestionResponse(rejected == 0 ? "accepted" : "partial", accepted, rejected, errors);
    }

    public static EventIngestionResponse duplicate() {
        return new EventIngestionResponse("duplicate", 0, 0, List.of());
    }

    public boolean isDuplicate() {
        return "duplicate".equalsIgnoreCase(status);
    }
}

