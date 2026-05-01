package com.rentflow.reservation.model;

import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

import java.time.ZonedDateTime;

public record ReservationSummary(
        ReservationId id,
        String reservationNumber,
        CustomerId customerId,
        VehicleId vehicleId,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        ReservationStatus status,
        Money totalAmount
) {
}
