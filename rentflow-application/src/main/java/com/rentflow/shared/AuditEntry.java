package com.rentflow.shared;

import com.rentflow.contract.DamageReportId;
import com.rentflow.payment.DepositId;
import com.rentflow.payment.RefundId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.InvoiceId;

import java.time.Instant;

public record AuditEntry(String actionType, String entityType, String entityId, String actor, Instant occurredAt) {
    public static AuditEntry of(String actionType, ReservationId id, StaffId actor) {
        return of(actionType, "Reservation", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, VehicleId id, StaffId actor) {
        return of(actionType, "Vehicle", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, VehicleCategoryId id, StaffId actor) {
        return of(actionType, "VehicleCategory", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, CustomerId id, StaffId actor) {
        return of(actionType, "Customer", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, ContractId id, StaffId actor) {
        return of(actionType, "Contract", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, DamageReportId id, StaffId actor) {
        return of(actionType, "DamageReport", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, InvoiceId id, StaffId actor) {
        return of(actionType, "Invoice", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, DepositId id, StaffId actor) {
        return of(actionType, "Deposit", id.value().toString(), actor);
    }

    public static AuditEntry of(String actionType, RefundId id, StaffId actor) {
        return of(actionType, "Refund", id.value().toString(), actor);
    }

    private static AuditEntry of(String actionType, String entityType, String entityId, StaffId actor) {
        return new AuditEntry(actionType, entityType, entityId, actor == null ? null : actor.value().toString(),
                Instant.now());
    }
}
