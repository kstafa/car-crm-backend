package com.rentflow.payment.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;

public record DepositActionRequest(@NotBlank String reason) {
}
