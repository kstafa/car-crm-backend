package com.rentflow.contract.adapter.in.rest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDamageReportRequest(
        @NotNull UUID vehicleId,
        UUID contractId,
        UUID customerId,
        @NotBlank String description,
        @NotNull String severity,
        @NotNull String liability,
        @NotNull @DecimalMin("0.01") BigDecimal estimatedCost
) {
}
