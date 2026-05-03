package com.rentflow.report.model;

import com.rentflow.shared.money.Money;

import java.time.YearMonth;

public record MonthlyRevenueRow(YearMonth month, Money revenue, int rentals) {
}
