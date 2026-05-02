package com.rentflow.payment;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Refund extends AggregateRoot {

    private final RefundId id;
    private final InvoiceId invoiceId;
    private final CustomerId customerId;
    private final Money amount;
    private final RefundReason reason;
    private RefundStatus status;
    private final PaymentMethod method;
    private final String notes;
    private final Instant requestedAt;
    private Instant processedAt;
    private StaffId approvedBy;
    private StaffId processedBy;

    private Refund(RefundId id, InvoiceId invoiceId, CustomerId customerId, Money amount, RefundReason reason,
                   RefundStatus status, PaymentMethod method, String notes, Instant requestedAt,
                   Instant processedAt, StaffId approvedBy, StaffId processedBy) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.method = method;
        this.notes = notes;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.approvedBy = approvedBy;
        this.processedBy = processedBy;
    }

    public static Refund request(InvoiceId invoiceId, CustomerId customerId, Money amount, RefundReason reason,
                                 PaymentMethod method, String notes) {
        Objects.requireNonNull(invoiceId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(method);
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Refund amount must be positive");
        }
        Refund refund = new Refund(RefundId.generate(), invoiceId, customerId, amount, reason, RefundStatus.PENDING,
                method, notes, Instant.now(), null, null, null);
        refund.registerEvent(new RefundRequestedEvent(refund.id, invoiceId, amount, reason));
        return refund;
    }

    public static Refund reconstitute(RefundId id, InvoiceId invoiceId, CustomerId customerId, Money amount,
                                      RefundReason reason, PaymentMethod method, String notes, RefundStatus status,
                                      StaffId approvedBy, StaffId processedBy, Instant requestedAt,
                                      Instant processedAt) {
        return new Refund(id, invoiceId, customerId, amount, reason, status, method, notes, requestedAt,
                processedAt, approvedBy, processedBy);
    }

    public void approve(StaffId approvedBy) {
        if (status != RefundStatus.PENDING) {
            throw new InvalidStateTransitionException("Can only approve a PENDING refund, current: " + status);
        }
        Objects.requireNonNull(approvedBy);
        this.approvedBy = approvedBy;
        status = RefundStatus.APPROVED;
        registerEvent(new RefundApprovedEvent(id, invoiceId, amount, approvedBy));
    }

    public void process(StaffId processedBy) {
        if (status != RefundStatus.APPROVED) {
            throw new InvalidStateTransitionException("Can only process an APPROVED refund, current: " + status);
        }
        Objects.requireNonNull(processedBy);
        this.processedBy = processedBy;
        status = RefundStatus.PROCESSED;
        processedAt = Instant.now();
        registerEvent(new RefundProcessedEvent(id, invoiceId, amount, processedBy));
    }

    public void reject(StaffId rejectedBy) {
        if (status != RefundStatus.PENDING) {
            throw new InvalidStateTransitionException("Can only reject a PENDING refund, current: " + status);
        }
        Objects.requireNonNull(rejectedBy);
        status = RefundStatus.REJECTED;
        registerEvent(new RefundRejectedEvent(id, invoiceId, rejectedBy));
    }

    public RefundId getId() {
        return id;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public Money getAmount() {
        return amount;
    }

    public RefundReason getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public StaffId getApprovedBy() {
        return approvedBy;
    }

    public StaffId getProcessedBy() {
        return processedBy;
    }
}
