package com.rentflow.contract;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.VehicleId;

import java.time.Instant;

public record DamageReportCreatedEvent(
        DamageReportId reportId,
        VehicleId vehicleId,
        DamageSeverity severity,
        Instant occurredAt
) implements DomainEvent {
    public DamageReportCreatedEvent(DamageReportId reportId, VehicleId vehicleId, DamageSeverity severity) {
        this(reportId, vehicleId, severity, Instant.now());
    }
}
