package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.fleet.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataVehicleRepo extends JpaRepository<VehicleJpaEntity, UUID> {
    List<VehicleJpaEntity> findByCategoryIdAndActiveTrue(UUID categoryId);

    @Query("""
            SELECT v FROM VehicleJpaEntity v
            WHERE (:status IS NULL OR v.status = :status)
              AND (:categoryId IS NULL OR v.categoryId = :categoryId)
              AND (:activeOnly = FALSE OR v.active = TRUE)
            """)
    Page<VehicleJpaEntity> findFiltered(
            @Param("status") VehicleStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable);
}
