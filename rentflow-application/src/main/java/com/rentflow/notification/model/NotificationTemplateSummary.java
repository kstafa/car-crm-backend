package com.rentflow.notification.model;

import com.rentflow.notification.NotificationChannel;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;

public record NotificationTemplateSummary(
        NotificationTemplateId id,
        String name,
        NotificationTrigger trigger,
        NotificationChannel channel,
        boolean active) {
}
