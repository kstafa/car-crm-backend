package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.RefundReason;
import com.rentflow.payment.RefundStatus;
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
@Table(name = "refunds")
class RefundJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    UUID invoiceId;
    @Column(nullable = false)
    UUID customerId;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal amount;
    @Column(nullable = false, length = 3)
    String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    RefundReason reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentMethod method;
    @Column(columnDefinition = "text")
    String notes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RefundStatus status;
    UUID approvedBy;
    UUID processedBy;
    @Column(nullable = false)
    Instant requestedAt;
    Instant processedAt;
    @Version
    Long version;
}
