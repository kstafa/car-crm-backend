package com.rentflow.fleet.port.out;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.query.ListVehiclesQuery;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    void save(Vehicle vehicle);

    Optional<Vehicle> findById(VehicleId id);

    List<Vehicle> findActiveByCategoryId(VehicleCategoryId categoryId);

    Page<VehicleSummary> findAll(ListVehiclesQuery query);
}
