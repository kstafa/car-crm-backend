package com.rentflow.reservation.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID customerId,
        @NotNull UUID vehicleId,
        @NotNull ZonedDateTime pickupDatetime,
        @NotNull ZonedDateTime returnDatetime,
        List<@Valid ExtraItemRequest> extras,
        String notes
) {
}
