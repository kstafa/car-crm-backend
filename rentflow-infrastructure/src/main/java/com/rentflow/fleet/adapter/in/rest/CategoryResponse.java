package com.rentflow.fleet.adapter.in.rest;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String description, BigDecimal baseDailyRate,
                               BigDecimal depositAmount, BigDecimal taxRate) {
}
