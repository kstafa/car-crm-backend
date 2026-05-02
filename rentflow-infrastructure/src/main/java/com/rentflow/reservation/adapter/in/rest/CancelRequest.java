package com.rentflow.reservation.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(@NotBlank String reason) {
}
