package com.rentflow.reservation.port.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;

import java.util.List;
import java.util.Optional;

public interface AvailabilityCachePort {
    Optional<List<VehicleId>> get(VehicleCategoryId categoryId, DateRange period);

    void put(VehicleCategoryId categoryId, DateRange period, List<VehicleId> ids);

    void invalidate(VehicleId vehicleId);
}
