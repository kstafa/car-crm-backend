package com.rentflow.reservation.command;

import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;

import java.time.ZonedDateTime;
import java.util.Objects;

public record CreateReservationCommand(
        CustomerId customerId,
        VehicleId vehicleId,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime,
        StaffId createdBy
) {
    public CreateReservationCommand {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(pickupDatetime);
        Objects.requireNonNull(returnDatetime);
        if (!returnDatetime.isAfter(pickupDatetime)) {
            throw new IllegalArgumentException("returnDatetime must be after pickupDatetime");
        }
    }
}
