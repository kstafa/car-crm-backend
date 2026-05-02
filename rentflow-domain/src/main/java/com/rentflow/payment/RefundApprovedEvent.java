package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record RefundApprovedEvent(
        RefundId id,
        InvoiceId invoiceId,
        Money amount,
        StaffId approvedBy,
        Instant occurredAt
) implements DomainEvent {
    public RefundApprovedEvent(RefundId id, InvoiceId invoiceId, Money amount, StaffId approvedBy) {
        this(id, invoiceId, amount, approvedBy, Instant.now());
    }
}
