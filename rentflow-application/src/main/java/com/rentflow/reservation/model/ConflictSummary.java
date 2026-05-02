package com.rentflow.reservation.model;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

public record ConflictSummary(
        ReservationId draftReservationId,
        String draftReservationNumber,
        VehicleId vehicleId,
        DateRange period,
        ReservationId conflictingReservationId,
        String conflictingReservationNumber,
        ReservationStatus conflictingStatus
) {
}
