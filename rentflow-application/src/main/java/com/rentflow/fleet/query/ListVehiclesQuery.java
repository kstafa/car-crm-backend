package com.rentflow.fleet.query;

import com.rentflow.fleet.VehicleStatus;
import com.rentflow.shared.id.VehicleCategoryId;

public record ListVehiclesQuery(
        VehicleStatus status,
        VehicleCategoryId categoryId,
        boolean activeOnly,
        int page,
        int size,
        String sortBy
) {
}
