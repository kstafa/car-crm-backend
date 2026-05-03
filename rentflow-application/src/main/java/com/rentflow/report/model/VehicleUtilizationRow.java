package com.rentflow.report.model;

import com.rentflow.shared.id.VehicleId;

public record VehicleUtilizationRow(
        VehicleId vehicleId,
        String licensePlate,
        String brand,
        String model,
        int rentedDays,
        double utilizationPercent) {
}
