package com.rentflow.contract.model;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.DamageReportStatus;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

public record DamageReportSummary(
        DamageReportId id,
        VehicleId vehicleId,
        ContractId contractId,
        String damageDescription,
        DamageSeverity severity,
        DamageReportStatus status,
        DamageLiability liability,
        Money estimatedCost
) {
}
