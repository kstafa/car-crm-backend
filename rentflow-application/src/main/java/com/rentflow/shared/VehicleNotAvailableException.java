package com.rentflow.shared;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.id.VehicleId;

public class VehicleNotAvailableException extends DomainException {
    private final VehicleId vehicleId;
    private final DateRange period;

    public VehicleNotAvailableException(VehicleId vehicleId, DateRange period) {
        super("Vehicle is not available for the requested period: " + vehicleId.value());
        this.vehicleId = vehicleId;
        this.period = period;
    }

    public VehicleId getVehicleId() {
        return vehicleId;
    }

    public DateRange getPeriod() {
        return period;
    }
}
