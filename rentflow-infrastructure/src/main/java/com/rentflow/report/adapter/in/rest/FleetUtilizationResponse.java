package com.rentflow.report.adapter.in.rest;

import java.time.LocalDate;
import java.util.List;

public record FleetUtilizationResponse(
        LocalDate from,
        LocalDate to,
        int totalVehicles,
        int totalDays,
        int rentedDays,
        double utilizationPercent,
        List<VehicleUtilizationRowResponse> rows) {
}
