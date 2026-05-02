package com.rentflow.fleet.model;

import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.money.Money;

import java.math.BigDecimal;

public record CategorySummary(
        VehicleCategoryId id,
        String name,
        String description,
        Money baseDailyRate,
        Money depositAmount,
        BigDecimal taxRate
) {
}
