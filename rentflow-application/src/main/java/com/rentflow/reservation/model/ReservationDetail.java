package com.rentflow.reservation.model;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

public record ReservationDetail(
        ReservationId id,
        String reservationNumber,
        CustomerId customerId,
        VehicleId vehicleId,
        DateRange rentalPeriod,
        ReservationStatus status,
        Money baseAmount,
        Money discountAmount,
        Money depositAmount,
        Money taxAmount,
        Money totalAmount,
        String notes
) {
}
