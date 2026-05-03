package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataContractRepo extends JpaRepository<ContractJpaEntity, UUID> {
    Optional<ContractJpaEntity> findByReservationId(UUID reservationId);

    @Query("""
            SELECT c FROM ContractJpaEntity c
            WHERE c.status = com.rentflow.contract.ContractStatus.ACTIVE
            ORDER BY c.scheduledReturn ASC
            """)
    List<ContractJpaEntity> findActive();

    @Query("""
            SELECT c FROM ContractJpaEntity c
            WHERE c.status = com.rentflow.contract.ContractStatus.ACTIVE
              AND c.scheduledReturn < :now
            ORDER BY c.scheduledReturn ASC
            """)
    List<ContractJpaEntity> findOverdue(@Param("now") java.time.ZonedDateTime now);

    long countByStatus(ContractStatus status);

    @Query("""
            SELECT new com.rentflow.report.model.VehicleUtilizationRaw(
                c.vehicleId, v.licensePlate, v.brand, v.model, COUNT(c.id))
            FROM ContractJpaEntity c
            JOIN VehicleJpaEntity v ON v.id = c.vehicleId
            WHERE c.status = com.rentflow.contract.ContractStatus.COMPLETED
              AND c.actualPickupDatetime >= :from
              AND c.actualReturnDatetime < :to
            GROUP BY c.vehicleId, v.licensePlate, v.brand, v.model
            """)
    List<com.rentflow.report.model.VehicleUtilizationRaw> vehicleUtilization(
            @Param("from") java.time.ZonedDateTime from,
            @Param("to") java.time.ZonedDateTime to);

    @Query("""
            SELECT c FROM ContractJpaEntity c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:vehicleId IS NULL OR c.vehicleId = :vehicleId)
              AND (:customerId IS NULL OR c.customerId = :customerId)
            ORDER BY c.scheduledPickup DESC
            """)
    Page<ContractJpaEntity> findFiltered(
            @Param("status") ContractStatus status,
            @Param("vehicleId") UUID vehicleId,
            @Param("customerId") UUID customerId,
            Pageable pageable);
}
