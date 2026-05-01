package com.rentflow.shared.adapter.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.id.VehicleId;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("!integration")
public class AlwaysAvailableVehicleAvailabilityAdapter implements VehicleAvailabilityPort {
    @Override
    public boolean isAvailable(VehicleId vehicleId, DateRange period) {
        return true;
    }
}
