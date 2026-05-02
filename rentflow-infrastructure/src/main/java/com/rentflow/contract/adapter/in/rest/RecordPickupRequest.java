package com.rentflow.contract.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RecordPickupRequest(
        @NotNull InspectionChecklistRequest preInspection,
        @NotNull String startFuelLevel,
        @Min(0) int startMileage,
        List<String> photoKeys
) {
}
