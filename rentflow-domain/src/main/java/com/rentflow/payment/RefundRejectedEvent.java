package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;

import java.time.Instant;

public record RefundRejectedEvent(
        RefundId id,
        InvoiceId invoiceId,
        StaffId rejectedBy,
        Instant occurredAt
) implements DomainEvent {
    public RefundRejectedEvent(RefundId id, InvoiceId invoiceId, StaffId rejectedBy) {
        this(id, invoiceId, rejectedBy, Instant.now());
    }
}
