package com.rentflow.reservation.command;

import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;

public record CancelReservationCommand(ReservationId reservationId, String reason, StaffId cancelledBy) {
}
