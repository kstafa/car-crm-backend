package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String method,
        String gatewayReference,
        Instant paidAt
) {
}
