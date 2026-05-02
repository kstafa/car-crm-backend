package com.rentflow.reservation.adapter.in.rest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExtraItemRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @Min(1) int quantity
) {
}
