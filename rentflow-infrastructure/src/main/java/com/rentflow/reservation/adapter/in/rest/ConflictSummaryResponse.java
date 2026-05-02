package com.rentflow.reservation.adapter.in.rest;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ConflictSummaryResponse(
        UUID draftReservationId,
        String draftReservationNumber,
        UUID vehicleId,
        ZonedDateTime draftStart,
        ZonedDateTime draftEnd,
        UUID conflictingReservationId,
        String conflictingReservationNumber,
        String conflictingStatus
) {
}
