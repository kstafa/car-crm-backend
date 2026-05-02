package com.rentflow.reservation.model;

import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ConflictRow(
        ReservationId draftId,
        String draftNumber,
        VehicleId vehicleId,
        ZonedDateTime draftStart,
        ZonedDateTime draftEnd,
        ReservationId conflictingId,
        String conflictingNumber,
        ReservationStatus conflictingStatus
) {
    public ConflictRow(UUID draftId, String draftNumber, UUID vehicleId, ZonedDateTime draftStart,
                       ZonedDateTime draftEnd, UUID conflictingId, String conflictingNumber,
                       ReservationStatus conflictingStatus) {
        this(ReservationId.of(draftId), draftNumber, VehicleId.of(vehicleId), draftStart, draftEnd,
                ReservationId.of(conflictingId), conflictingNumber, conflictingStatus);
    }
}
