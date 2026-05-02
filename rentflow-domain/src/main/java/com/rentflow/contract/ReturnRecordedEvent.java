package com.rentflow.contract;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;
import java.time.ZonedDateTime;

public record ReturnRecordedEvent(
        ContractId contractId,
        VehicleId vehicleId,
        ZonedDateTime actualReturn,
        boolean hasDamage,
        Instant occurredAt
) implements DomainEvent {
    public ReturnRecordedEvent(ContractId contractId, VehicleId vehicleId, ZonedDateTime actualReturn,
                               boolean hasDamage) {
        this(contractId, vehicleId, actualReturn, hasDamage, Instant.now());
    }
}
