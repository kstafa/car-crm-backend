package com.rentflow.contract.command;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.contract.InspectionChecklist;
import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;

import java.util.List;
import java.util.Objects;

public record RecordReturnCommand(
        ContractId contractId,
        InspectionChecklist postInspection,
        FuelLevel endFuelLevel,
        int endMileage,
        List<String> photoKeys,
        String damageDescription,
        DamageSeverity damageSeverity,
        DamageLiability damageLiability,
        Money estimatedDamageCost,
        StaffId performedBy
) {
    public RecordReturnCommand {
        Objects.requireNonNull(contractId);
        Objects.requireNonNull(postInspection);
        Objects.requireNonNull(endFuelLevel);
        if (endMileage < 0) {
            throw new IllegalArgumentException("endMileage must be >= 0");
        }
        photoKeys = photoKeys == null ? List.of() : List.copyOf(photoKeys);
    }
}
