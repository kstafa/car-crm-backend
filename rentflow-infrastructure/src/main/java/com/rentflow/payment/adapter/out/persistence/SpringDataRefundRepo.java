package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataRefundRepo extends JpaRepository<RefundJpaEntity, UUID> {

    @Query("""
            SELECT r FROM RefundJpaEntity r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:customerId IS NULL OR r.customerId = :customerId)
            ORDER BY r.requestedAt DESC
            """)
    Page<RefundJpaEntity> findFiltered(
            @Param("status") RefundStatus status,
            @Param("customerId") UUID customerId,
            Pageable pageable);
}
