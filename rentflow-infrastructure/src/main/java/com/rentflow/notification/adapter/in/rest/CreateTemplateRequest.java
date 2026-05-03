package com.rentflow.notification.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
        @NotBlank String name,
        @NotNull String trigger,
        @NotNull String channel,
        String subjectTemplate,
        @NotBlank String bodyTemplate) {
}
