package com.rentflow.notification.adapter.in.rest;

import java.time.Instant;
import java.util.UUID;

public record StaffNotificationResponse(
        UUID id,
        String title,
        String message,
        String entityType,
        String entityId,
        boolean read,
        Instant createdAt) {
}
