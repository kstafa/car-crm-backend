package com.rentflow.reservation.adapter.in.rest;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationDetailResponse(
        UUID id,
        String reservationNumber,
        UUID customerId,
        String customerName,
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        String status,
        BigDecimal baseAmount,
        BigDecimal discountAmount,
        BigDecimal depositAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        String notes,
        List<ExtraItemResponse> extras
) {
}
