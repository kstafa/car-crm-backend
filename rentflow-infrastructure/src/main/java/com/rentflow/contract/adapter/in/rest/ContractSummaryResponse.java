package com.rentflow.contract.adapter.in.rest;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ContractSummaryResponse(
        UUID id,
        String contractNumber,
        UUID reservationId,
        UUID customerId,
        String customerName,
        UUID vehicleId,
        String vehicleLicensePlate,
        ZonedDateTime scheduledPickup,
        ZonedDateTime scheduledReturn,
        ZonedDateTime actualPickupDatetime,
        ZonedDateTime actualReturnDatetime,
        String status
) {
}
