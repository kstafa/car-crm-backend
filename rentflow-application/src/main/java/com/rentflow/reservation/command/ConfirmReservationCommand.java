package com.rentflow.reservation.command;

import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;

public record ConfirmReservationCommand(ReservationId reservationId, StaffId confirmedBy) {
}
