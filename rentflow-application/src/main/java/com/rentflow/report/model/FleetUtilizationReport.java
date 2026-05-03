package com.rentflow.report.model;

import java.time.LocalDate;
import java.util.List;

public record FleetUtilizationReport(
        LocalDate from,
        LocalDate to,
        int totalVehicles,
        int totalDays,
        int rentedDays,
        double utilizationPercent,
        List<VehicleUtilizationRow> rows) {
}
