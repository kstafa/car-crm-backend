package com.rentflow.notification.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;

public record UpdateTemplateRequest(String newSubject, @NotBlank String newBody) {
}
