package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositSummaryResponse(
        UUID id,
        UUID contractId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String status,
        Instant heldAt,
        Instant settledAt
) {
}
