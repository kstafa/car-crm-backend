package com.rentflow.report.adapter.in.rest;

import java.math.BigDecimal;

public record MonthlyRevenueRowResponse(String month, BigDecimal revenue, int rentals, String currency) {
}
