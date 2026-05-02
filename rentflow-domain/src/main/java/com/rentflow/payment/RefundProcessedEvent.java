package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record RefundProcessedEvent(
        RefundId id,
        InvoiceId invoiceId,
        Money amount,
        StaffId processedBy,
        Instant occurredAt
) implements DomainEvent {
    public RefundProcessedEvent(RefundId id, InvoiceId invoiceId, Money amount, StaffId processedBy) {
        this(id, invoiceId, amount, processedBy, Instant.now());
    }
}
