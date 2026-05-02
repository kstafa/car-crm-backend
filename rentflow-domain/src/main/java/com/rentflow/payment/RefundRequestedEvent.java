package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record RefundRequestedEvent(
        RefundId id,
        InvoiceId invoiceId,
        Money amount,
        RefundReason reason,
        Instant occurredAt
) implements DomainEvent {
    public RefundRequestedEvent(RefundId id, InvoiceId invoiceId, Money amount, RefundReason reason) {
        this(id, invoiceId, amount, reason, Instant.now());
    }
}
