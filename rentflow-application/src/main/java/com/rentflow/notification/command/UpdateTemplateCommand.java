package com.rentflow.notification.command;

import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTemplateCommand(
        @NotNull NotificationTemplateId templateId,
        String newSubject,
        @NotBlank String newBody,
        StaffId updatedBy) {
}
