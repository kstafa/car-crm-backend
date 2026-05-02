package com.rentflow.fleet.command;

import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;

import java.util.Objects;

public record RegisterVehicleCommand(
        String licensePlate,
        String brand,
        String model,
        int year,
        VehicleCategoryId categoryId,
        int initialMileage,
        String description,
        StaffId registeredBy
) {
    public RegisterVehicleCommand {
        Objects.requireNonNull(licensePlate);
        Objects.requireNonNull(brand);
        Objects.requireNonNull(model);
        Objects.requireNonNull(categoryId);
        if (licensePlate.isBlank()) {
            throw new IllegalArgumentException("licensePlate must not be blank");
        }
        if (brand.isBlank()) {
            throw new IllegalArgumentException("brand must not be blank");
        }
        if (model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (year < 1900) {
            throw new IllegalArgumentException("year must be >= 1900");
        }
        if (initialMileage < 0) {
            throw new IllegalArgumentException("initialMileage must be >= 0");
        }
    }
}
