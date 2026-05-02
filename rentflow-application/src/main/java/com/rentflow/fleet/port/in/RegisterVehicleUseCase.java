package com.rentflow.fleet.port.in;

import com.rentflow.fleet.command.RegisterVehicleCommand;
import com.rentflow.shared.id.VehicleId;

public interface RegisterVehicleUseCase {
    VehicleId register(RegisterVehicleCommand cmd);
}
