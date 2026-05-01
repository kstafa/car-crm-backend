package com.rentflow.reservation;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ReservationId;

import java.time.Instant;

public record ReservationExtendedEvent(ReservationId reservationId, DateRange newPeriod, Instant occurredAt)
        implements DomainEvent {
    public ReservationExtendedEvent(ReservationId reservationId, DateRange newPeriod) {
        this(reservationId, newPeriod, Instant.now());
    }
}
