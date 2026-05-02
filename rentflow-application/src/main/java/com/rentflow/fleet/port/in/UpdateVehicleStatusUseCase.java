package com.rentflow.fleet.port.in;

import com.rentflow.fleet.command.UpdateVehicleStatusCommand;

public interface UpdateVehicleStatusUseCase {
    void update(UpdateVehicleStatusCommand cmd);
}
