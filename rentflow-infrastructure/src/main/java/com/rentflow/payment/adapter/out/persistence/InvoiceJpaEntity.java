package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.PaymentMethod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
class InvoiceJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false, length = 20)
    String invoiceNumber;
    @Column(nullable = false)
    UUID contractId;
    @Column(nullable = false)
    UUID customerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    InvoiceStatus status;
    @Column(nullable = false)
    LocalDate issueDate;
    @Column(nullable = false)
    LocalDate dueDate;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal paidAmount;
    @Column(nullable = false, length = 3)
    String currency;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    List<InvoiceLineItemJpaEntity> lineItems = new ArrayList<>();
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paidAt ASC")
    List<InvoicePaymentJpaEntity> payments = new ArrayList<>();
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}

@Entity
@Table(name = "invoice_line_items")
class InvoiceLineItemJpaEntity {
    @Id
    UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    InvoiceJpaEntity invoice;
    @Column(name = "invoice_id", insertable = false, updatable = false)
    UUID invoiceId;
    @Column(nullable = false, columnDefinition = "text")
    String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    InvoiceLineItemType type;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal unitPrice;
    @Column(nullable = false, length = 3)
    String currency;
    int quantity;
    @Column(name = "sort_order", nullable = false)
    int sortOrder;
}

@Entity
@Table(name = "invoice_payments")
class InvoicePaymentJpaEntity {
    @Id
    UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    InvoiceJpaEntity invoice;
    @Column(name = "invoice_id", insertable = false, updatable = false)
    UUID invoiceId;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal amount;
    @Column(nullable = false, length = 3)
    String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentMethod method;
    String gatewayReference;
    @Column(nullable = false)
    Instant paidAt;
}
