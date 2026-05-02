package com.rentflow.reservation.adapter.in.rest;

import java.time.ZonedDateTime;
import java.util.UUID;

public record CalendarEntryResponse(
        UUID reservationId,
        String reservationNumber,
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        UUID customerId,
        String customerName,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        String status
) {
}
