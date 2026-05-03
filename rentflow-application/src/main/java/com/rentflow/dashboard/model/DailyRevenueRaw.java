package com.rentflow.dashboard.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueRaw(LocalDate date, BigDecimal amount) {
}
