package com.rentflow.fleet.port.out;

import com.rentflow.fleet.VehicleCategory;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.shared.id.VehicleCategoryId;

import java.util.List;
import java.util.Optional;

public interface VehicleCategoryRepository {
    void save(VehicleCategory category);

    Optional<VehicleCategory> findById(VehicleCategoryId id);

    Optional<VehicleCategory> findByName(String name);

    List<CategorySummary> findAllActive();
}
