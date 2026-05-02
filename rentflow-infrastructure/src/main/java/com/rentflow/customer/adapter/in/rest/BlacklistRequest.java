package com.rentflow.customer.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;

public record BlacklistRequest(@NotBlank String reason) {
}
