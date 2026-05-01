package com.rentflow.fleet.port.out;

import com.rentflow.fleet.Vehicle;
import com.rentflow.shared.id.VehicleId;

import java.util.Optional;

public interface VehicleRepository {
    void save(Vehicle vehicle);

    Optional<Vehicle> findById(VehicleId id);
}
