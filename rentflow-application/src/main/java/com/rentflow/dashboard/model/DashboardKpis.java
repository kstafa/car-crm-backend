package com.rentflow.dashboard.model;

import com.rentflow.shared.money.Money;

public record DashboardKpis(
        int activeRentalsToday,
        int availableVehiclesNow,
        int pickupsTodayCount,
        int returnsTodayCount,
        int overdueReturnsCount,
        Money revenueThisMonth,
        Money revenueLastMonth,
        double revenueChangePercent) {
}
