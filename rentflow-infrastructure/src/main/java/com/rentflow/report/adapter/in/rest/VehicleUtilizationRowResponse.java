package com.rentflow.report.adapter.in.rest;

import java.util.UUID;

public record VehicleUtilizationRowResponse(
        UUID vehicleId,
        String licensePlate,
        String brand,
        String model,
        int rentedDays,
        double utilizationPercent) {
}
