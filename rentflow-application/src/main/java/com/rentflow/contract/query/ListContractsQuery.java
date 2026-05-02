package com.rentflow.contract.query;

import com.rentflow.contract.ContractStatus;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.VehicleId;

public record ListContractsQuery(
        ContractStatus status,
        VehicleId vehicleId,
        CustomerId customerId,
        int page,
        int size
) {
}
