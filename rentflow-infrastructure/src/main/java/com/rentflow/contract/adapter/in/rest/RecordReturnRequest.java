package com.rentflow.contract.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RecordReturnRequest(
        @NotNull InspectionChecklistRequest postInspection,
        @NotNull String endFuelLevel,
        @Min(0) int endMileage,
        List<String> photoKeys,
        String damageDescription,
        String damageSeverity,
        String damageLiability,
        BigDecimal estimatedDamageCost
) {
}
