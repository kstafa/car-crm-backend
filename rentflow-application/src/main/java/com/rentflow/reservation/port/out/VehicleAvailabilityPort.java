package com.rentflow.reservation.port.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.id.VehicleId;

public interface VehicleAvailabilityPort {
    boolean isAvailable(VehicleId vehicleId, DateRange period);
}
