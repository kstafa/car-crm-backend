package com.rentflow.contract.adapter.in.rest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenContractRequest(@NotNull UUID reservationId) {
}
