package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.Contract;
import com.rentflow.contract.Inspection;
import com.rentflow.contract.InspectionChecklist;
import com.rentflow.contract.model.ContractDetail;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class ContractJpaMapper {

    public ContractJpaEntity toJpa(Contract domain) {
        ContractJpaEntity entity = new ContractJpaEntity();
        entity.id = domain.getId().value();
        entity.contractNumber = domain.getContractNumber();
        entity.reservationId = domain.getReservationId().value();
        entity.customerId = domain.getCustomerId().value();
        entity.vehicleId = domain.getVehicleId().value();
        entity.scheduledPickup = domain.getScheduledPickup();
        entity.scheduledReturn = domain.getScheduledReturn();
        entity.actualPickupDatetime = domain.getActualPickupDatetime();
        entity.actualReturnDatetime = domain.getActualReturnDatetime();
        entity.status = domain.getStatus();
        entity.signatureKey = domain.getSignatureKey();
        entity.inspections = new ArrayList<>();
        if (domain.getPreInspection() != null) {
            entity.inspections.add(toInspectionJpa(domain.getPreInspection(), entity.id));
        }
        if (domain.getPostInspection() != null) {
            entity.inspections.add(toInspectionJpa(domain.getPostInspection(), entity.id));
        }
        return entity;
    }

    public Contract toDomain(ContractJpaEntity entity) {
        Inspection pre = null;
        Inspection post = null;
        for (InspectionJpaEntity inspection : entity.inspections) {
            Inspection domain = toInspectionDomain(inspection);
            if (domain.type() == Inspection.InspectionType.PRE) {
                pre = domain;
            } else {
                post = domain;
            }
        }
        return Contract.reconstitute(
                ContractId.of(entity.id),
                entity.contractNumber,
                ReservationId.of(entity.reservationId),
                CustomerId.of(entity.customerId),
                VehicleId.of(entity.vehicleId),
                normalize(entity.scheduledPickup),
                normalize(entity.scheduledReturn),
                entity.status,
                pre,
                post,
                normalize(entity.actualPickupDatetime),
                normalize(entity.actualReturnDatetime),
                entity.signatureKey
        );
    }

    public ContractSummary toSummary(ContractJpaEntity entity) {
        return new ContractSummary(
                ContractId.of(entity.id),
                entity.contractNumber,
                ReservationId.of(entity.reservationId),
                CustomerId.of(entity.customerId),
                null,
                VehicleId.of(entity.vehicleId),
                null,
                normalize(entity.scheduledPickup),
                normalize(entity.scheduledReturn),
                normalize(entity.actualPickupDatetime),
                normalize(entity.actualReturnDatetime),
                entity.status
        );
    }

    public ContractDetail toDetail(ContractJpaEntity entity) {
        Contract domain = toDomain(entity);
        return new ContractDetail(domain.getId(), domain.getContractNumber(), domain.getReservationId(),
                domain.getCustomerId(), domain.getVehicleId(), domain.getScheduledPickup(),
                domain.getScheduledReturn(), domain.getActualPickupDatetime(), domain.getActualReturnDatetime(),
                domain.getStatus(), domain.getPreInspection(), domain.getPostInspection(), domain.getSignatureKey());
    }

    InspectionJpaEntity toInspectionJpa(Inspection inspection, UUID contractId) {
        InspectionJpaEntity entity = new InspectionJpaEntity();
        entity.id = UUID.randomUUID();
        entity.contractId = contractId;
        entity.type = inspection.type().name();
        entity.frontOk = inspection.checklist().frontOk();
        entity.rearOk = inspection.checklist().rearOk();
        entity.leftSideOk = inspection.checklist().leftSideOk();
        entity.rightSideOk = inspection.checklist().rightSideOk();
        entity.interiorOk = inspection.checklist().interiorOk();
        entity.trunkOk = inspection.checklist().trunkOk();
        entity.tiresOk = inspection.checklist().tiresOk();
        entity.lightsOk = inspection.checklist().lightsOk();
        entity.notes = inspection.checklist().notes();
        entity.fuelLevel = inspection.fuelLevel();
        entity.mileage = inspection.mileage();
        entity.performedAt = inspection.performedAt();
        entity.performedBy = inspection.performedBy().value();
        entity.photoKeys = new ArrayList<>(inspection.photoKeys());
        return entity;
    }

    Inspection toInspectionDomain(InspectionJpaEntity entity) {
        return new Inspection(
                Inspection.InspectionType.valueOf(entity.type),
                new InspectionChecklist(entity.frontOk, entity.rearOk, entity.leftSideOk, entity.rightSideOk,
                        entity.interiorOk, entity.trunkOk, entity.tiresOk, entity.lightsOk, entity.notes),
                entity.fuelLevel,
                entity.mileage,
                entity.photoKeys,
                entity.performedAt,
                StaffId.of(entity.performedBy)
        );
    }

    private static java.time.ZonedDateTime normalize(java.time.ZonedDateTime timestamp) {
        return timestamp == null ? null : timestamp.withZoneSameInstant(ZoneId.of("UTC"));
    }
}
