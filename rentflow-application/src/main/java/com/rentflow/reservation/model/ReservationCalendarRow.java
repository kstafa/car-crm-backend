package com.rentflow.reservation.model;

import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ReservationCalendarRow(
        ReservationId reservationId,
        String reservationNumber,
        VehicleId vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        CustomerId customerId,
        String customerFirstName,
        String customerLastName,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        ReservationStatus status
) {
    public ReservationCalendarRow(UUID reservationId, String reservationNumber, UUID vehicleId,
                                  String vehicleLicensePlate, String vehicleBrand, String vehicleModel,
                                  UUID customerId, String customerFirstName, String customerLastName,
                                  ZonedDateTime pickupDatetime, ZonedDateTime returnDatetime,
                                  ReservationStatus status) {
        this(ReservationId.of(reservationId), reservationNumber, VehicleId.of(vehicleId), vehicleLicensePlate,
                vehicleBrand, vehicleModel, CustomerId.of(customerId), customerFirstName, customerLastName,
                pickupDatetime, returnDatetime, status);
    }
}
