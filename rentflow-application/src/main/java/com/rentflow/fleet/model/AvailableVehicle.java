package com.rentflow.fleet.model;

import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

public record AvailableVehicle(
        VehicleId id,
        String licensePlate,
        String brand,
        String model,
        int year,
        String categoryName,
        Money dailyRate,
        String thumbnailKey
) {
}
