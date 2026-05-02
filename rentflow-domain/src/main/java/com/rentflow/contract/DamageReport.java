package com.rentflow.contract;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DamageReport extends AggregateRoot {

    private final DamageReportId id;
    private VehicleId vehicleId;
    private ContractId contractId;
    private CustomerId customerId;
    private String damageDescription;
    private DamageSeverity severity;
    private DamageReportStatus status;
    private DamageLiability liability;
    private Money estimatedCost;
    private Money actualCost;
    private List<String> photoKeys;
    private Instant reportedAt;

    private DamageReport(DamageReportId id, VehicleId vehicleId, ContractId contractId, CustomerId customerId,
                         String damageDescription, DamageSeverity severity, DamageReportStatus status,
                         DamageLiability liability, Money estimatedCost, Money actualCost, List<String> photoKeys,
                         Instant reportedAt) {
        this.id = Objects.requireNonNull(id);
        this.vehicleId = Objects.requireNonNull(vehicleId);
        this.contractId = contractId;
        this.customerId = customerId;
        this.damageDescription = Objects.requireNonNull(damageDescription);
        this.severity = Objects.requireNonNull(severity);
        this.status = Objects.requireNonNull(status);
        this.liability = Objects.requireNonNull(liability);
        this.estimatedCost = Objects.requireNonNull(estimatedCost);
        this.actualCost = actualCost;
        this.photoKeys = new ArrayList<>(photoKeys == null ? List.of() : photoKeys);
        this.reportedAt = Objects.requireNonNull(reportedAt);
    }

    public static DamageReport report(VehicleId vehicleId, ContractId contractId, CustomerId customerId,
                                      String description, DamageSeverity severity, DamageLiability liability,
                                      Money estimatedCost) {
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(liability);
        Objects.requireNonNull(estimatedCost);
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        DamageReport report = new DamageReport(DamageReportId.generate(), vehicleId, contractId, customerId,
                description, severity, DamageReportStatus.OPEN, liability, estimatedCost, null, List.of(),
                Instant.now());
        report.registerEvent(new DamageReportCreatedEvent(report.id, vehicleId, severity));
        return report;
    }

    public static DamageReport reconstitute(DamageReportId id, VehicleId vehicleId, ContractId contractId,
                                            CustomerId customerId, String damageDescription,
                                            DamageSeverity severity, DamageReportStatus status,
                                            DamageLiability liability, Money estimatedCost, Money actualCost,
                                            List<String> photoKeys, Instant reportedAt) {
        return new DamageReport(id, vehicleId, contractId, customerId, damageDescription, severity, status,
                liability, estimatedCost, actualCost, photoKeys, reportedAt);
    }

    public void addPhoto(String key) {
        if (key == null || key.isBlank()) {
            throw new DomainException("photo key must not be blank");
        }
        photoKeys.add(key);
    }

    public void settle(Money actualCost) {
        if (status == DamageReportStatus.CLOSED) {
            throw new InvalidStateTransitionException("Report is already closed");
        }
        Objects.requireNonNull(actualCost);
        this.actualCost = actualCost;
        this.status = DamageReportStatus.SETTLED;
    }

    public void close() {
        if (status != DamageReportStatus.SETTLED && status != DamageReportStatus.OPEN) {
            throw new InvalidStateTransitionException("Can only close an OPEN or SETTLED report");
        }
        this.status = DamageReportStatus.CLOSED;
    }

    public void startRepair() {
        if (status != DamageReportStatus.OPEN) {
            throw new InvalidStateTransitionException("Can only start repair on an OPEN report");
        }
        this.status = DamageReportStatus.UNDER_REPAIR;
    }

    public DamageReportId getId() {
        return id;
    }

    public VehicleId getVehicleId() {
        return vehicleId;
    }

    public ContractId getContractId() {
        return contractId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getDamageDescription() {
        return damageDescription;
    }

    public DamageSeverity getSeverity() {
        return severity;
    }

    public DamageReportStatus getStatus() {
        return status;
    }

    public DamageLiability getLiability() {
        return liability;
    }

    public Money getEstimatedCost() {
        return estimatedCost;
    }

    public Money getActualCost() {
        return actualCost;
    }

    public List<String> getPhotoKeys() {
        return Collections.unmodifiableList(photoKeys);
    }

    public Instant getReportedAt() {
        return reportedAt;
    }
}
