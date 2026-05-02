package com.rentflow.contract.model;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.DamageReportStatus;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

import java.time.Instant;
import java.util.List;

public record DamageReportDetail(
        DamageReportId id,
        VehicleId vehicleId,
        ContractId contractId,
        CustomerId customerId,
        String damageDescription,
        DamageSeverity severity,
        DamageReportStatus status,
        DamageLiability liability,
        Money estimatedCost,
        Money actualCost,
        List<String> photoKeys,
        Instant reportedAt
) {
    public DamageReportDetail {
        photoKeys = photoKeys == null ? List.of() : List.copyOf(photoKeys);
    }
}
