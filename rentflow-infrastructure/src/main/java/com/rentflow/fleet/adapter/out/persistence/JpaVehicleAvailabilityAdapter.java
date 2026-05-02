package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.adapter.out.persistence.SpringDataReservationRepo;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Primary
@RequiredArgsConstructor
public class JpaVehicleAvailabilityAdapter implements VehicleAvailabilityPort {

    private final SpringDataReservationRepo reservationRepo;
    private final SpringDataVehicleRepo vehicleRepo;

    @Override
    public boolean isAvailable(VehicleId vehicleId, DateRange period) {
        return reservationRepo.findConflicting(vehicleId.value(), period.start(), period.end()).isEmpty();
    }

    @Override
    public List<VehicleId> findConflictingVehicleIds(VehicleCategoryId categoryId, DateRange period) {
        List<UUID> vehicleIds = vehicleRepo.findByCategoryIdAndActiveTrue(categoryId.value())
                .stream()
                .map(entity -> entity.id)
                .toList();
        if (vehicleIds.isEmpty()) {
            return List.of();
        }
        return reservationRepo.findConflictingVehicleIds(vehicleIds, period.start(), period.end())
                .stream()
                .map(VehicleId::of)
                .toList();
    }
}
