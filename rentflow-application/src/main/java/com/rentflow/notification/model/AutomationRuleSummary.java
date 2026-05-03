package com.rentflow.notification.model;

import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;

public record AutomationRuleSummary(
        AutomationRuleId id,
        String name,
        NotificationTrigger trigger,
        NotificationTemplateId templateId,
        boolean active,
        int delayMinutes) {
}
