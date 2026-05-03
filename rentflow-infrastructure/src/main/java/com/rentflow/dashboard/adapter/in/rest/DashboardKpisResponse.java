package com.rentflow.dashboard.adapter.in.rest;

import java.math.BigDecimal;

public record DashboardKpisResponse(
        int activeRentalsToday,
        int availableVehiclesNow,
        int pickupsTodayCount,
        int returnsTodayCount,
        int overdueReturnsCount,
        BigDecimal revenueThisMonth,
        BigDecimal revenueLastMonth,
        double revenueChangePercent,
        String currency) {
}
