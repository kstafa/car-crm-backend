package com.rentflow.contract;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;
import java.time.ZonedDateTime;

public record PickupRecordedEvent(
        ContractId contractId,
        VehicleId vehicleId,
        ZonedDateTime actualPickup,
        Instant occurredAt
) implements DomainEvent {
    public PickupRecordedEvent(ContractId contractId, VehicleId vehicleId, ZonedDateTime actualPickup) {
        this(contractId, vehicleId, actualPickup, Instant.now());
    }
}
