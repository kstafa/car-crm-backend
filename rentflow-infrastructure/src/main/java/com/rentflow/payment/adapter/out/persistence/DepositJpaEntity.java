package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.DepositStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deposits")
class DepositJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    UUID contractId;
    @Column(nullable = false)
    UUID customerId;
    @Column(nullable = false)
    UUID invoiceId;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal amount;
    @Column(nullable = false, length = 3)
    String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    DepositStatus status;
    @Column(columnDefinition = "text")
    String releaseReason;
    @Column(columnDefinition = "text")
    String forfeitReason;
    @Column(nullable = false)
    Instant heldAt;
    Instant settledAt;
    @Version
    Long version;
}
