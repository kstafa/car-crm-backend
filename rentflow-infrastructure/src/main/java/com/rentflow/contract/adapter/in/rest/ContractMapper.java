package com.rentflow.contract.adapter.in.rest;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.contract.Inspection;
import com.rentflow.contract.InspectionChecklist;
import com.rentflow.contract.command.ExtendContractCommand;
import com.rentflow.contract.command.OpenContractCommand;
import com.rentflow.contract.command.RecordPickupCommand;
import com.rentflow.contract.command.RecordReturnCommand;
import com.rentflow.contract.model.ContractDetail;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.model.ReturnSummary;
import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class ContractMapper {

    private static final Currency EUR = Currency.getInstance("EUR");

    public OpenContractCommand toCommand(OpenContractRequest request, StaffId staffId) {
        return new OpenContractCommand(ReservationId.of(request.reservationId()), staffId);
    }

    public RecordPickupCommand toCommand(ContractId id, RecordPickupRequest request, StaffId staffId) {
        return new RecordPickupCommand(id, checklist(request.preInspection()),
                FuelLevel.valueOf(request.startFuelLevel()), request.startMileage(), request.photoKeys(), staffId);
    }

    public RecordReturnCommand toCommand(ContractId id, RecordReturnRequest request, StaffId staffId) {
        DamageSeverity severity = request.damageSeverity() == null ? null : DamageSeverity.valueOf(request.damageSeverity());
        DamageLiability liability = request.damageLiability() == null ? null : DamageLiability.valueOf(request.damageLiability());
        Money estimatedCost = request.estimatedDamageCost() == null ? null : new Money(request.estimatedDamageCost(), EUR);
        return new RecordReturnCommand(id, checklist(request.postInspection()), FuelLevel.valueOf(request.endFuelLevel()),
                request.endMileage(), request.photoKeys(), request.damageDescription(), severity, liability,
                estimatedCost, staffId);
    }

    public ExtendContractCommand toCommand(ContractId id, ExtendContractRequest request, StaffId staffId) {
        return new ExtendContractCommand(id, request.newScheduledReturn(), staffId);
    }

    public ContractSummaryResponse toSummaryResponse(ContractSummary summary) {
        return new ContractSummaryResponse(summary.id().value(), summary.contractNumber(),
                summary.reservationId().value(), summary.customerId().value(), summary.customerName(),
                summary.vehicleId().value(), summary.vehicleLicensePlate(), summary.scheduledPickup(),
                summary.scheduledReturn(), summary.actualPickupDatetime(), summary.actualReturnDatetime(),
                summary.status().name());
    }

    public ContractDetailResponse toDetailResponse(ContractDetail detail) {
        return new ContractDetailResponse(detail.id().value(), detail.contractNumber(),
                detail.reservationId().value(), detail.customerId().value(), detail.vehicleId().value(),
                detail.scheduledPickup(), detail.scheduledReturn(), detail.actualPickupDatetime(),
                detail.actualReturnDatetime(), detail.status().name(), toResponse(detail.preInspection()),
                toResponse(detail.postInspection()), detail.signatureKey());
    }

    public ReturnSummaryResponse toResponse(ReturnSummary summary) {
        return new ReturnSummaryResponse(summary.contractId().value(), summary.damageDetected(),
                summary.damageReportId() == null ? null : summary.damageReportId().value(),
                summary.lateFee().amount(), summary.fuelSurcharge().amount(), summary.totalSurcharges().amount(),
                summary.totalSurcharges().currency().getCurrencyCode(),
                summary.invoiceId() == null ? null : summary.invoiceId().value());
    }

    private static InspectionChecklist checklist(InspectionChecklistRequest request) {
        return new InspectionChecklist(request.frontOk(), request.rearOk(), request.leftSideOk(),
                request.rightSideOk(), request.interiorOk(), request.trunkOk(), request.tiresOk(),
                request.lightsOk(), request.notes());
    }

    private static InspectionResponse toResponse(Inspection inspection) {
        if (inspection == null) {
            return null;
        }
        return new InspectionResponse(inspection.type().name(), inspection.checklist().frontOk(),
                inspection.checklist().rearOk(), inspection.checklist().leftSideOk(),
                inspection.checklist().rightSideOk(), inspection.checklist().interiorOk(),
                inspection.checklist().trunkOk(), inspection.checklist().tiresOk(),
                inspection.checklist().lightsOk(), inspection.checklist().notes(), inspection.fuelLevel().name(),
                inspection.mileage(), inspection.photoKeys(), inspection.performedAt());
    }
}
