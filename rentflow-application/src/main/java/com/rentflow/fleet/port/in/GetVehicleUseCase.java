package com.rentflow.fleet.port.in;

import com.rentflow.fleet.model.VehicleDetail;
import com.rentflow.shared.id.VehicleId;

public interface GetVehicleUseCase {
    VehicleDetail get(VehicleId id);
}
