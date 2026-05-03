package com.rentflow.report.model;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExportReportCommand(
        @NotNull String reportType,
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotNull String format) {
}
