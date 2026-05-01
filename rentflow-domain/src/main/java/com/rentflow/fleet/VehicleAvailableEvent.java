package com.rentflow.fleet;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;

public record VehicleAvailableEvent(VehicleId vehicleId, Instant occurredAt) implements DomainEvent {
    public VehicleAvailableEvent(VehicleId vehicleId) {
        this(vehicleId, Instant.now());
    }
}
