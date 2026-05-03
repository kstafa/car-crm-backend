package com.rentflow.report.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;

public record MonthlyRevenueRaw(Object monthTrunc, BigDecimal amount, long rentalCount) {
    public YearMonth month() {
        if (monthTrunc instanceof LocalDate localDate) {
            return YearMonth.from(localDate);
        }
        if (monthTrunc instanceof Date date) {
            return YearMonth.from(date.toLocalDate());
        }
        if (monthTrunc instanceof Timestamp timestamp) {
            return YearMonth.from(timestamp.toLocalDateTime());
        }
        return YearMonth.parse(monthTrunc.toString().substring(0, 7));
    }
}
