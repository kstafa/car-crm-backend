package com.rentflow.dashboard.model;

import com.rentflow.shared.money.Money;

import java.time.LocalDate;

public record DailyRevenue(LocalDate date, Money amount) {
}
