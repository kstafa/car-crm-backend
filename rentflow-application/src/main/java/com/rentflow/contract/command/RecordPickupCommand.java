package com.rentflow.contract.command;

import com.rentflow.contract.InspectionChecklist;
import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.StaffId;

import java.util.List;
import java.util.Objects;

public record RecordPickupCommand(
        ContractId contractId,
        InspectionChecklist preInspection,
        FuelLevel startFuelLevel,
        int startMileage,
        List<String> photoKeys,
        StaffId performedBy
) {
    public RecordPickupCommand {
        Objects.requireNonNull(contractId);
        Objects.requireNonNull(preInspection);
        Objects.requireNonNull(startFuelLevel);
        if (startMileage < 0) {
            throw new IllegalArgumentException("startMileage must be >= 0");
        }
        photoKeys = photoKeys == null ? List.of() : List.copyOf(photoKeys);
    }
}
