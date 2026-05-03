package com.rentflow.report.model;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

public record ReportPeriodQuery(
        @NotNull LocalDate from,
        @NotNull LocalDate to) {
    public ReportPeriodQuery {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from");
        }
    }
}
