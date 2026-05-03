package com.rentflow.notification.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAutomationRuleRequest(
        @NotBlank String name,
        @NotNull String trigger,
        @NotNull UUID templateId,
        @Min(0) int delayMinutes) {
}
