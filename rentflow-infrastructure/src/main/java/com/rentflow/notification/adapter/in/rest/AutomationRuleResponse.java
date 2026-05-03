package com.rentflow.notification.adapter.in.rest;

import java.util.UUID;

public record AutomationRuleResponse(
        UUID id,
        String name,
        String trigger,
        UUID templateId,
        boolean active,
        int delayMinutes) {
}
