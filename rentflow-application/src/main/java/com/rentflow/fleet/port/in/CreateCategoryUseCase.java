package com.rentflow.fleet.port.in;

import com.rentflow.fleet.command.CreateCategoryCommand;
import com.rentflow.shared.id.VehicleCategoryId;

public interface CreateCategoryUseCase {
    VehicleCategoryId create(CreateCategoryCommand cmd);
}
