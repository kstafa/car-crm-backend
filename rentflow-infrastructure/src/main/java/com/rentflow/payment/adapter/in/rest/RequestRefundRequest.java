package com.rentflow.payment.adapter.in.rest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RequestRefundRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull String reason,
        @NotNull String method,
        String notes
) {
}
