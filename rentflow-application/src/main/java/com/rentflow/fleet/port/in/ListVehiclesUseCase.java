package com.rentflow.fleet.port.in;

import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.query.ListVehiclesQuery;
import org.springframework.data.domain.Page;

public interface ListVehiclesUseCase {
    Page<VehicleSummary> list(ListVehiclesQuery q);
}
