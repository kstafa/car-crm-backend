package com.rentflow.dashboard.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueResponse(LocalDate date, BigDecimal amount, String currency) {
}
