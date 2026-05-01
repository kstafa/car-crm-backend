package com.rentflow.shared.adapter.out;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.shared.id.VehicleId;

import java.util.Optional;

public class NoOpVehicleRepository implements VehicleRepository {
    @Override
    public void save(Vehicle vehicle) {
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id) {
        return Optional.empty();
    }
}
