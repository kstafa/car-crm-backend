package com.rentflow.notification.model;

import com.rentflow.notification.NotificationId;

import java.time.Instant;

public record StaffNotificationSummary(
        NotificationId id,
        String title,
        String message,
        String entityType,
        String entityId,
        boolean read,
        Instant createdAt) {
}
