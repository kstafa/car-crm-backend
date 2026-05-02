package com.rentflow.shared.adapter.out;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.fleet.query.ListVehiclesQuery;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public class NoOpVehicleRepository implements VehicleRepository {
    @Override
    public void save(Vehicle vehicle) {
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id) {
        return Optional.empty();
    }

    @Override
    public List<Vehicle> findActiveByCategoryId(VehicleCategoryId categoryId) {
        return List.of();
    }

    @Override
    public Page<VehicleSummary> findAll(ListVehiclesQuery query) {
        int page = query == null ? 0 : Math.max(query.page(), 0);
        int size = query == null ? 20 : Math.max(query.size(), 1);
        return Page.empty(PageRequest.of(page, size));
    }
}
