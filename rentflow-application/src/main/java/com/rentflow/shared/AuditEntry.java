package com.rentflow.shared;

import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;

import java.time.Instant;

public record AuditEntry(String actionType, String entityType, String entityId, String actor, Instant occurredAt) {
    public static AuditEntry of(String actionType, ReservationId id, StaffId actor) {
        return new AuditEntry(
                actionType,
                "Reservation",
                id.value().toString(),
                actor == null ? null : actor.value().toString(),
                Instant.now()
        );
    }
}
