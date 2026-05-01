package com.rentflow.reservation;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ReservationId;

import java.time.Instant;

public record ReservationCompletedEvent(ReservationId reservationId, Instant occurredAt) implements DomainEvent {
    public ReservationCompletedEvent(ReservationId reservationId) {
        this(reservationId, Instant.now());
    }
}
