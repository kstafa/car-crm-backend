package com.rentflow.fleet.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataVehicleCategoryRepo extends JpaRepository<VehicleCategoryJpaEntity, UUID> {
    Optional<VehicleCategoryJpaEntity> findByName(String name);

    List<VehicleCategoryJpaEntity> findByActiveTrue();
}
