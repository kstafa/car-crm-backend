package com.rentflow.report.model;

import com.rentflow.shared.money.Money;

import java.time.LocalDate;
import java.util.List;

public record RevenueReport(
        LocalDate from,
        LocalDate to,
        Money totalRevenue,
        Money averagePerRental,
        int completedRentals,
        List<MonthlyRevenueRow> monthly) {
}
