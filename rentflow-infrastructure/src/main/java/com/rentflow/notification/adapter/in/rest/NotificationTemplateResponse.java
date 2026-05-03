package com.rentflow.notification.adapter.in.rest;

import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String name,
        String trigger,
        String channel,
        boolean active,
        String subjectTemplate,
        String bodyTemplate) {
}
