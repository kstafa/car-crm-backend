package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;

import java.time.Instant;

public record InvoiceSentEvent(InvoiceId id, CustomerId customerId, Instant occurredAt) implements DomainEvent {
    public InvoiceSentEvent(InvoiceId id, CustomerId customerId) {
        this(id, customerId, Instant.now());
    }
}
