package com.rentflow.reservation.port.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;

import java.util.List;

public interface VehicleAvailabilityPort {
    boolean isAvailable(VehicleId vehicleId, DateRange period);

    List<VehicleId> findConflictingVehicleIds(VehicleCategoryId categoryId, DateRange period);
}
