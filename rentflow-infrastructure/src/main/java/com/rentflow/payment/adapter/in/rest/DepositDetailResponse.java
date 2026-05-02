package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositDetailResponse(
        UUID id,
        UUID contractId,
        UUID customerId,
        UUID invoiceId,
        BigDecimal amount,
        String currency,
        String status,
        String releaseReason,
        String forfeitReason,
        Instant heldAt,
        Instant settledAt
) {
}
