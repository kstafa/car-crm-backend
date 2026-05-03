package com.rentflow.notification.command;

import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAutomationRuleCommand(
        @NotBlank String name,
        @NotNull NotificationTrigger trigger,
        @NotNull NotificationTemplateId templateId,
        int delayMinutes,
        StaffId createdBy) {
}
