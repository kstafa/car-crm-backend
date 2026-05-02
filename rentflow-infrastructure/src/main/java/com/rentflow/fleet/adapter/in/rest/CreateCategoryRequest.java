package com.rentflow.fleet.adapter.in.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateCategoryRequest(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal baseDailyRate,
        @NotNull BigDecimal depositAmount,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal taxRate
) {
}
