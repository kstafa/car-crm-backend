package com.rentflow.contract.adapter.in.rest;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageReportStatus;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.contract.command.CreateDamageReportCommand;
import com.rentflow.contract.model.DamageReportDetail;
import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.contract.query.ListDamageReportsQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.UUID;

@Component
public class DamageReportMapper {

    private static final Currency EUR = Currency.getInstance("EUR");

    public CreateDamageReportCommand toCommand(CreateDamageReportRequest request, StaffId staffId) {
        return new CreateDamageReportCommand(
                VehicleId.of(request.vehicleId()),
                request.contractId() == null ? null : ContractId.of(request.contractId()),
                request.customerId() == null ? null : CustomerId.of(request.customerId()),
                request.description(),
                DamageSeverity.valueOf(request.severity()),
                DamageLiability.valueOf(request.liability()),
                new Money(request.estimatedCost(), EUR),
                staffId
        );
    }

    public ListDamageReportsQuery toQuery(String status, UUID vehicleId, int page, int size) {
        return new ListDamageReportsQuery(status == null ? null : DamageReportStatus.valueOf(status),
                vehicleId == null ? null : VehicleId.of(vehicleId), page, size);
    }

    public DamageReportSummaryResponse toSummaryResponse(DamageReportSummary summary) {
        return new DamageReportSummaryResponse(summary.id().value(), summary.vehicleId().value(),
                summary.contractId() == null ? null : summary.contractId().value(), summary.damageDescription(),
                summary.severity().name(), summary.status().name(), summary.liability().name(),
                summary.estimatedCost().amount(), summary.estimatedCost().currency().getCurrencyCode());
    }

    public DamageReportDetailResponse toDetailResponse(DamageReportDetail detail) {
        return new DamageReportDetailResponse(detail.id().value(), detail.vehicleId().value(),
                detail.contractId() == null ? null : detail.contractId().value(),
                detail.customerId() == null ? null : detail.customerId().value(), detail.damageDescription(),
                detail.severity().name(), detail.status().name(), detail.liability().name(),
                detail.estimatedCost().amount(), detail.actualCost() == null ? null : detail.actualCost().amount(),
                detail.estimatedCost().currency().getCurrencyCode(), detail.photoKeys(), detail.reportedAt());
    }
}
