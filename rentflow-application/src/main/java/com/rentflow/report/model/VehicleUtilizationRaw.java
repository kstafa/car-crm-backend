package com.rentflow.report.model;

import java.util.UUID;

public record VehicleUtilizationRaw(
        UUID vehicleId,
        String licensePlate,
        String brand,
        String model,
        long rentedDays) {
}
