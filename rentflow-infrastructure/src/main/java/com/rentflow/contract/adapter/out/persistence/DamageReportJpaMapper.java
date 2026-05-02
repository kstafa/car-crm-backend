package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.DamageReport;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.model.DamageReportDetail;
import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Currency;

@Component
public class DamageReportJpaMapper {

    public DamageReportJpaEntity toJpa(DamageReport domain) {
        DamageReportJpaEntity entity = new DamageReportJpaEntity();
        entity.id = domain.getId().value();
        entity.vehicleId = domain.getVehicleId().value();
        entity.contractId = domain.getContractId() == null ? null : domain.getContractId().value();
        entity.customerId = domain.getCustomerId() == null ? null : domain.getCustomerId().value();
        entity.damageDescription = domain.getDamageDescription();
        entity.severity = domain.getSeverity();
        entity.status = domain.getStatus();
        entity.liability = domain.getLiability();
        entity.estimatedCost = domain.getEstimatedCost().amount();
        entity.currency = domain.getEstimatedCost().currency().getCurrencyCode();
        entity.actualCost = domain.getActualCost() == null ? null : domain.getActualCost().amount();
        entity.reportedAt = domain.getReportedAt();
        entity.photoKeys = new ArrayList<>(domain.getPhotoKeys());
        return entity;
    }

    public DamageReport toDomain(DamageReportJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return DamageReport.reconstitute(
                DamageReportId.of(entity.id),
                VehicleId.of(entity.vehicleId),
                entity.contractId == null ? null : ContractId.of(entity.contractId),
                entity.customerId == null ? null : CustomerId.of(entity.customerId),
                entity.damageDescription,
                entity.severity,
                entity.status,
                entity.liability,
                new Money(entity.estimatedCost, currency),
                entity.actualCost == null ? null : new Money(entity.actualCost, currency),
                entity.photoKeys,
                entity.reportedAt
        );
    }

    public DamageReportSummary toSummary(DamageReportJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return new DamageReportSummary(
                DamageReportId.of(entity.id),
                VehicleId.of(entity.vehicleId),
                entity.contractId == null ? null : ContractId.of(entity.contractId),
                entity.damageDescription,
                entity.severity,
                entity.status,
                entity.liability,
                new Money(entity.estimatedCost, currency)
        );
    }

    public DamageReportDetail toDetail(DamageReportJpaEntity entity) {
        DamageReport report = toDomain(entity);
        return new DamageReportDetail(report.getId(), report.getVehicleId(), report.getContractId(),
                report.getCustomerId(), report.getDamageDescription(), report.getSeverity(), report.getStatus(),
                report.getLiability(), report.getEstimatedCost(), report.getActualCost(), report.getPhotoKeys(),
                report.getReportedAt());
    }
}
