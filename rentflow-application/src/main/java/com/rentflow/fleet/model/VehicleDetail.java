package com.rentflow.fleet.model;

import com.rentflow.fleet.VehicleStatus;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;

import java.util.List;

public record VehicleDetail(
        VehicleId id,
        String licensePlate,
        String brand,
        String model,
        int year,
        VehicleStatus status,
        VehicleCategoryId categoryId,
        String categoryName,
        int currentMileage,
        boolean active,
        String description,
        List<String> photoKeys
) {
}
