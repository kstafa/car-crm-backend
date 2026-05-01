package com.rentflow.reservation;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;

public record ReservationConfirmedEvent(
        ReservationId reservationId,
        CustomerId customerId,
        VehicleId vehicleId,
        DateRange rentalPeriod,
        Instant occurredAt
) implements DomainEvent {
    public ReservationConfirmedEvent(ReservationId reservationId, CustomerId customerId, VehicleId vehicleId,
                                     DateRange rentalPeriod) {
        this(reservationId, customerId, vehicleId, rentalPeriod, Instant.now());
    }
}
