package com.rentflow.contract.adapter.in.rest;

import java.math.BigDecimal;
import java.util.UUID;

public record DamageReportSummaryResponse(
        UUID id,
        UUID vehicleId,
        UUID contractId,
        String damageDescription,
        String severity,
        String status,
        String liability,
        BigDecimal estimatedCost,
        String currency
) {
}
