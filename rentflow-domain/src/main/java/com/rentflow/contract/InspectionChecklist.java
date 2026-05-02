package com.rentflow.contract;

public record InspectionChecklist(
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
    public static InspectionChecklist allOk() {
        return new InspectionChecklist(true, true, true, true, true, true, true, true, null);
    }

    public boolean hasDamage() {
        return !frontOk || !rearOk || !leftSideOk || !rightSideOk
                || !interiorOk || !trunkOk || !tiresOk || !lightsOk;
    }
}
