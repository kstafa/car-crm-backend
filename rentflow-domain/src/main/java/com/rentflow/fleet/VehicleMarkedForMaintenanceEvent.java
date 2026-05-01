package com.rentflow.fleet;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;

public record VehicleMarkedForMaintenanceEvent(VehicleId vehicleId, Instant occurredAt) implements DomainEvent {
    public VehicleMarkedForMaintenanceEvent(VehicleId vehicleId) {
        this(vehicleId, Instant.now());
    }
}
