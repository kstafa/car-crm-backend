package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundSummaryResponse(
        UUID id,
        UUID invoiceId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String reason,
        String status,
        Instant requestedAt
) {
}
