package com.rentflow.fleet.command;

import com.rentflow.fleet.VehicleStatus;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;

public record UpdateVehicleStatusCommand(VehicleId vehicleId, VehicleStatus newStatus, StaffId updatedBy) {
}
