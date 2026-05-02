package com.rentflow.fleet.adapter.in.rest;

import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.UUID;

public record AvailabilityRequest(
        @NotNull UUID categoryId,
        @NotNull ZonedDateTime pickupDatetime,
        @NotNull ZonedDateTime returnDatetime
) {
}
