package com.rentflow.contract.adapter.in.rest;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ContractDetailResponse(
        UUID id,
        String contractNumber,
        UUID reservationId,
        UUID customerId,
        UUID vehicleId,
        ZonedDateTime scheduledPickup,
        ZonedDateTime scheduledReturn,
        ZonedDateTime actualPickupDatetime,
        ZonedDateTime actualReturnDatetime,
        String status,
        InspectionResponse preInspection,
        InspectionResponse postInspection,
        String signatureKey
) {
}
