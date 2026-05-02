package com.rentflow.fleet;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.VehicleCategoryId;

import java.time.Instant;

public record VehicleCategoryCreatedEvent(VehicleCategoryId id, String name, Instant occurredAt)
        implements DomainEvent {
    public VehicleCategoryCreatedEvent(VehicleCategoryId id, String name) {
        this(id, name, Instant.now());
    }
}
