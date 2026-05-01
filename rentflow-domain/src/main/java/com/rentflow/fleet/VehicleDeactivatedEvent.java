package com.rentflow.fleet;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;

public record VehicleDeactivatedEvent(VehicleId vehicleId, Instant occurredAt) implements DomainEvent {
    public VehicleDeactivatedEvent(VehicleId vehicleId) {
        this(vehicleId, Instant.now());
    }
}
