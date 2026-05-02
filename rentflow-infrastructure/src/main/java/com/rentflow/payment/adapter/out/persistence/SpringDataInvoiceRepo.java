package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataInvoiceRepo extends JpaRepository<InvoiceJpaEntity, UUID> {

    Optional<InvoiceJpaEntity> findByContractId(UUID contractId);

    @Query("""
            SELECT i FROM InvoiceJpaEntity i
            WHERE i.status IN (com.rentflow.payment.InvoiceStatus.SENT,
                               com.rentflow.payment.InvoiceStatus.PARTIALLY_PAID)
              AND i.dueDate < :today
            ORDER BY i.dueDate ASC
            """)
    List<InvoiceJpaEntity> findOverdue(@Param("today") LocalDate today);

    @Query("""
            SELECT i FROM InvoiceJpaEntity i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:contractId IS NULL OR i.contractId = :contractId)
              AND (:from IS NULL OR i.issueDate >= :from)
              AND (:to IS NULL OR i.issueDate <= :to)
            ORDER BY i.issueDate DESC
            """)
    Page<InvoiceJpaEntity> findFiltered(
            @Param("status") InvoiceStatus status,
            @Param("customerId") UUID customerId,
            @Param("contractId") UUID contractId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    Page<InvoiceJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);
}
