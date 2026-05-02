package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.DepositStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataDepositRepo extends JpaRepository<DepositJpaEntity, UUID> {
    Optional<DepositJpaEntity> findByContractId(UUID contractId);

    @Query("""
            SELECT d FROM DepositJpaEntity d
            WHERE (:status IS NULL OR d.status = :status)
              AND (:customerId IS NULL OR d.customerId = :customerId)
            ORDER BY d.heldAt DESC
            """)
    Page<DepositJpaEntity> findFiltered(
            @Param("status") DepositStatus status,
            @Param("customerId") UUID customerId,
            Pageable pageable);
}
