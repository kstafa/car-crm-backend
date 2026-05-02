package com.rentflow.contract.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DamageReportDetailResponse(
        UUID id,
        UUID vehicleId,
        UUID contractId,
        UUID customerId,
        String damageDescription,
        String severity,
        String status,
        String liability,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        String currency,
        List<String> photoKeys,
        Instant reportedAt
) {
}
