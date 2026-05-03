package com.rentflow.shared;

import com.rentflow.shared.id.StaffId;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditEntry(
        String actionType,
        String entityType,
        String entityId,
        String actor,
        Instant occurredAt,
        String beforeValue,
        String afterValue,
        String ipAddress) {

    public AuditEntry {
        Objects.requireNonNull(actionType);
        Objects.requireNonNull(occurredAt);
    }

    public static AuditEntry of(String actionType, Object entityId, StaffId actor) {
        return new AuditEntry(
                actionType,
                entityId != null ? entityType(entityId) : null,
                entityId != null ? entityValue(entityId) : null,
                actor != null ? actor.value().toString() : "SYSTEM",
                Instant.now(),
                null,
                null,
                null);
    }

    public static AuditEntry of(String actionType, String entityType, String entityId, String actor) {
        return new AuditEntry(actionType, entityType, entityId, actor, Instant.now(), null, null, null);
    }

    private static String entityType(Object entityId) {
        String simpleName = entityId.getClass().getSimpleName();
        return simpleName.endsWith("Id") ? simpleName.substring(0, simpleName.length() - 2) : simpleName;
    }

    private static String entityValue(Object entityId) {
        if (entityId instanceof UUID uuid) {
            return uuid.toString();
        }
        try {
            Method value = entityId.getClass().getMethod("value");
            Object raw = value.invoke(entityId);
            return raw == null ? null : raw.toString();
        } catch (ReflectiveOperationException ignored) {
            return entityId.toString();
        }
    }
}
