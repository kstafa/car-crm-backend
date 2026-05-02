package com.rentflow.contract.query;

import com.rentflow.contract.DamageReportStatus;
import com.rentflow.shared.id.VehicleId;

public record ListDamageReportsQuery(
        DamageReportStatus status,
        VehicleId vehicleId,
        int page,
        int size
) {
}
