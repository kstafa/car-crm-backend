package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record InvoicePaidEvent(InvoiceId invoiceId, CustomerId customerId, Money totalAmount, Instant occurredAt)
        implements DomainEvent {
    public InvoicePaidEvent(InvoiceId invoiceId, CustomerId customerId, Money totalAmount) {
        this(invoiceId, customerId, totalAmount, Instant.now());
    }
}
