package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.DamageReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataDamageReportRepo extends JpaRepository<DamageReportJpaEntity, UUID> {
    @Query("""
            SELECT d FROM DamageReportJpaEntity d
            WHERE (:status IS NULL OR d.status = :status)
              AND (:vehicleId IS NULL OR d.vehicleId = :vehicleId)
            ORDER BY d.reportedAt DESC
            """)
    Page<DamageReportJpaEntity> findFiltered(
            @Param("status") DamageReportStatus status,
            @Param("vehicleId") UUID vehicleId,
            Pageable pageable);

    List<DamageReportJpaEntity> findByVehicleId(UUID vehicleId);
}
