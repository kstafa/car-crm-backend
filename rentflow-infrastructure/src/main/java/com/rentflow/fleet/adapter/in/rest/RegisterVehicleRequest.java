package com.rentflow.fleet.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterVehicleRequest(
        @NotBlank String licensePlate,
        @NotBlank String brand,
        @NotBlank String model,
        @Min(1900) int year,
        @NotNull UUID categoryId,
        @Min(0) int initialMileage,
        String description
) {
}
