package com.rentflow.reservation;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;

import java.time.Instant;

public record ReservationCancelledEvent(
        ReservationId reservationId,
        CustomerId customerId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
    public ReservationCancelledEvent(ReservationId reservationId, CustomerId customerId, String reason) {
        this(reservationId, customerId, reason, Instant.now());
    }
}
