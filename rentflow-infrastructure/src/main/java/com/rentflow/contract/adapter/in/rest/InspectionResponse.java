package com.rentflow.contract.adapter.in.rest;

import java.time.Instant;
import java.util.List;

public record InspectionResponse(
        String type,
        boolean frontOk,
        boolean rearOk,
        boolean leftSideOk,
        boolean rightSideOk,
        boolean interiorOk,
        boolean trunkOk,
        boolean tiresOk,
        boolean lightsOk,
        String notes,
        String fuelLevel,
        int mileage,
        List<String> photoKeys,
        Instant performedAt
) {
}
