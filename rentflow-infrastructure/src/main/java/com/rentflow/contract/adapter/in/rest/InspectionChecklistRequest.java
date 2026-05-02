package com.rentflow.contract.adapter.in.rest;

public record InspectionChecklistRequest(
        boolean frontOk,
        boolean rearOk,
        boolean leftSideOk,
        boolean rightSideOk,
        boolean interiorOk,
        boolean trunkOk,
        boolean tiresOk,
        boolean lightsOk,
        String notes
) {
}
