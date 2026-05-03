package com.rentflow.report.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        BigDecimal averagePerRental,
        int completedRentals,
        String currency,
        List<MonthlyRevenueRowResponse> monthly) {
}
