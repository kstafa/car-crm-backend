package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class VehicleJpaMapper {

    public VehicleJpaEntity toJpa(Vehicle domain) {
        var entity = new VehicleJpaEntity();
        entity.id = domain.getId().value();
        entity.licensePlate = domain.getLicensePlate();
        entity.brand = domain.getBrand();
        entity.model = domain.getModel();
        entity.year = domain.getYear();
        entity.currentMileage = domain.getCurrentMileage();
        entity.status = domain.getStatus();
        entity.categoryId = domain.getCategoryId().value();
        entity.active = domain.isActive();
        entity.description = domain.getDescription();
        entity.photoKeys = new ArrayList<>(domain.getPhotoKeys());
        return entity;
    }

    public Vehicle toDomain(VehicleJpaEntity e) {
        return Vehicle.reconstitute(
                VehicleId.of(e.id),
                e.licensePlate,
                e.brand,
                e.model,
                e.year,
                VehicleCategoryId.of(e.categoryId),
                e.currentMileage,
                e.status,
                e.active,
                e.description,
                e.photoKeys
        );
    }

    public VehicleSummary toSummary(VehicleJpaEntity e) {
        String thumbnailKey = e.photoKeys == null || e.photoKeys.isEmpty() ? null : e.photoKeys.getFirst();
        return new VehicleSummary(
                VehicleId.of(e.id),
                e.licensePlate,
                e.brand,
                e.model,
                e.year,
                e.status,
                null,
                e.currentMileage,
                e.active,
                thumbnailKey
        );
    }
}
