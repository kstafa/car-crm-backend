package com.rentflow.reservation.command;

import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;

import java.time.ZonedDateTime;
import java.util.Objects;

public record ExtendReservationCommand(
        ReservationId reservationId,
        ZonedDateTime newReturnDatetime,
        StaffId extendedBy
) {
    public ExtendReservationCommand {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(newReturnDatetime, "newReturnDatetime must not be null");
        Objects.requireNonNull(extendedBy, "extendedBy must not be null");
    }
}
