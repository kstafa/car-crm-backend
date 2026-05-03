package com.rentflow.notification.command;

import com.rentflow.notification.NotificationTrigger;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SendNotificationCommand(
        @NotNull NotificationTrigger trigger,
        @NotNull Map<String, String> variables,
        String recipientEmail,
        String recipientPhone,
        StaffId recipientStaffId,
        String entityType,
        String entityId) {
}
