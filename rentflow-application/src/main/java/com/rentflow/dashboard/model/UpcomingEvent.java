package com.rentflow.dashboard.model;

import java.time.ZonedDateTime;

public record UpcomingEvent(
        String type,
        String referenceId,
        String description,
        ZonedDateTime scheduledAt) {
}
