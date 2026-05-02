package com.rentflow.fleet.port.in;

import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;

import java.util.List;

public interface FindAvailableVehiclesUseCase {
    List<AvailableVehicle> find(FindAvailableVehiclesQuery q);
}
