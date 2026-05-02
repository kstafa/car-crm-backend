package com.rentflow.fleet.model;

import com.rentflow.fleet.VehicleStatus;
import com.rentflow.shared.id.VehicleId;

public record VehicleSummary(
        VehicleId id,
        String licensePlate,
        String brand,
        String model,
        int year,
        VehicleStatus status,
        String categoryName,
        int currentMileage,
        boolean active,
        String thumbnailKey
) {
}
