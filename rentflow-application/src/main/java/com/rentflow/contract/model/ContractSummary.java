package com.rentflow.contract.model;

import com.rentflow.contract.ContractStatus;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.ZonedDateTime;

public record ContractSummary(
        ContractId id,
        String contractNumber,
        ReservationId reservationId,
        CustomerId customerId,
        String customerName,
        VehicleId vehicleId,
        String vehicleLicensePlate,
        ZonedDateTime scheduledPickup,
        ZonedDateTime scheduledReturn,
        ZonedDateTime actualPickupDatetime,
        ZonedDateTime actualReturnDatetime,
        ContractStatus status
) {
}
