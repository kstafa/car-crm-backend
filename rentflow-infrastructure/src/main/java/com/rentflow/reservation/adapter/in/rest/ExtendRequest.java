package com.rentflow.reservation.adapter.in.rest;

import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record ExtendRequest(@NotNull ZonedDateTime newReturnDatetime) {
}
