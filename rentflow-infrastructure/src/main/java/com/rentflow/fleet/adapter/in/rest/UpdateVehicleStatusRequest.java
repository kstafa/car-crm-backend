package com.rentflow.fleet.adapter.in.rest;

import com.rentflow.fleet.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatusRequest(@NotNull VehicleStatus status) {
}
