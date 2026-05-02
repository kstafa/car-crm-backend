package com.rentflow.reservation.adapter.in.rest;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public record ReservationListResponse(
        UUID id,
        String reservationNumber,
        UUID customerId,
        String customerName,
        UUID vehicleId,
        String vehicleLicensePlate,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        String status,
        BigDecimal totalAmount,
        String currency
) {
}
