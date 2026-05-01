package com.rentflow.shared.adapter.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
public class NoOpAvailabilityCache implements AvailabilityCachePort {
    @Override
    public Optional<List<VehicleId>> get(VehicleCategoryId categoryId, DateRange period) {
        return Optional.empty();
    }

    @Override
    public void put(VehicleCategoryId categoryId, DateRange period, List<VehicleId> ids) {
    }

    @Override
    public void invalidate(VehicleId vehicleId) {
    }
}
