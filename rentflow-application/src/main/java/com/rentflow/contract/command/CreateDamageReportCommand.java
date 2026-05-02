package com.rentflow.contract.command;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

import java.util.Objects;

public record CreateDamageReportCommand(
        VehicleId vehicleId,
        ContractId contractId,
        CustomerId customerId,
        String description,
        DamageSeverity severity,
        DamageLiability liability,
        Money estimatedCost,
        StaffId reportedBy
) {
    public CreateDamageReportCommand {
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(liability);
        Objects.requireNonNull(estimatedCost);
    }
}
