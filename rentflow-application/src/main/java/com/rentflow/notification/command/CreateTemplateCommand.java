package com.rentflow.notification.command;

import com.rentflow.notification.NotificationChannel;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateCommand(
        @NotBlank String name,
        @NotNull NotificationTrigger trigger,
        @NotNull NotificationChannel channel,
        String subjectTemplate,
        @NotBlank String bodyTemplate,
        StaffId createdBy) {
}
