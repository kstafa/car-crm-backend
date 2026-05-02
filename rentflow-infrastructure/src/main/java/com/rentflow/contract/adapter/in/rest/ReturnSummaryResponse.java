package com.rentflow.contract.adapter.in.rest;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnSummaryResponse(
        UUID contractId,
        boolean damageDetected,
        UUID damageReportId,
        BigDecimal lateFee,
        BigDecimal fuelSurcharge,
        BigDecimal totalSurcharges,
        String currency
) {
}
