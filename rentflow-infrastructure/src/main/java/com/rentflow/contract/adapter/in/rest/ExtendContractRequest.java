package com.rentflow.contract.adapter.in.rest;

import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record ExtendContractRequest(@NotNull ZonedDateTime newScheduledReturn) {
}
