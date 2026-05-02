package com.rentflow.contract;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;

public record ContractOpenedEvent(
        ContractId contractId,
        ReservationId reservationId,
        VehicleId vehicleId,
        CustomerId customerId,
        Instant occurredAt
) implements DomainEvent {
    public ContractOpenedEvent(ContractId contractId, ReservationId reservationId, VehicleId vehicleId,
                               CustomerId customerId) {
        this(contractId, reservationId, vehicleId, customerId, Instant.now());
    }
}
