package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record PaymentRecordedEvent(InvoiceId invoiceId, Money amount, PaymentMethod paymentMethod, Instant occurredAt)
        implements DomainEvent {
    public PaymentRecordedEvent(InvoiceId invoiceId, Money amount, PaymentMethod paymentMethod) {
        this(invoiceId, amount, paymentMethod, Instant.now());
    }
}
