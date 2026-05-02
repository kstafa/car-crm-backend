package com.rentflow.contract.model;

import com.rentflow.contract.ContractStatus;
import com.rentflow.contract.Inspection;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.ZonedDateTime;

public record ContractDetail(
        ContractId id,
        String contractNumber,
        ReservationId reservationId,
        CustomerId customerId,
        VehicleId vehicleId,
        ZonedDateTime scheduledPickup,
        ZonedDateTime scheduledReturn,
        ZonedDateTime actualPickupDatetime,
        ZonedDateTime actualReturnDatetime,
        ContractStatus status,
        Inspection preInspection,
        Inspection postInspection,
        String signatureKey
) {
}
