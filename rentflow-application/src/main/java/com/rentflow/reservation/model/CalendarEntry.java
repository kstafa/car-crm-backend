package com.rentflow.reservation.model;

import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.ZonedDateTime;

public record CalendarEntry(
        ReservationId reservationId,
        String reservationNumber,
        VehicleId vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        CustomerId customerId,
        String customerName,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        ReservationStatus status
) {
}
