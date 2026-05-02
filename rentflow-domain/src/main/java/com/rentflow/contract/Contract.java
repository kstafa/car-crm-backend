package com.rentflow.contract;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Objects;

public final class Contract extends AggregateRoot {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CONTRACT_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final ContractId id;
    private final String contractNumber;
    private ReservationId reservationId;
    private CustomerId customerId;
    private VehicleId vehicleId;
    private ZonedDateTime scheduledPickup;
    private ZonedDateTime scheduledReturn;
    private ContractStatus status;
    private Inspection preInspection;
    private Inspection postInspection;
    private ZonedDateTime actualPickupDatetime;
    private ZonedDateTime actualReturnDatetime;
    private String signatureKey;

    private Contract(ContractId id, String contractNumber, ReservationId reservationId, CustomerId customerId,
                     VehicleId vehicleId, ZonedDateTime scheduledPickup, ZonedDateTime scheduledReturn,
                     ContractStatus status, Inspection preInspection, Inspection postInspection,
                     ZonedDateTime actualPickupDatetime, ZonedDateTime actualReturnDatetime, String signatureKey) {
        this.id = Objects.requireNonNull(id);
        this.contractNumber = Objects.requireNonNull(contractNumber);
        this.reservationId = Objects.requireNonNull(reservationId);
        this.customerId = Objects.requireNonNull(customerId);
        this.vehicleId = Objects.requireNonNull(vehicleId);
        this.scheduledPickup = Objects.requireNonNull(scheduledPickup);
        this.scheduledReturn = Objects.requireNonNull(scheduledReturn);
        this.status = Objects.requireNonNull(status);
        this.preInspection = preInspection;
        this.postInspection = postInspection;
        this.actualPickupDatetime = actualPickupDatetime;
        this.actualReturnDatetime = actualReturnDatetime;
        this.signatureKey = signatureKey;
    }

    public static Contract open(ReservationId reservationId, CustomerId customerId, VehicleId vehicleId,
                                ZonedDateTime scheduledPickup, ZonedDateTime scheduledReturn) {
        Contract contract = new Contract(
                ContractId.generate(),
                "CON-" + randomContractSuffix(),
                reservationId,
                customerId,
                vehicleId,
                scheduledPickup,
                scheduledReturn,
                ContractStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null
        );
        contract.registerEvent(new ContractOpenedEvent(contract.id, reservationId, vehicleId, customerId));
        return contract;
    }

    public static Contract reconstitute(ContractId id, String contractNumber, ReservationId reservationId,
                                        CustomerId customerId, VehicleId vehicleId, ZonedDateTime scheduledPickup,
                                        ZonedDateTime scheduledReturn, ContractStatus status,
                                        Inspection preInspection, Inspection postInspection,
                                        ZonedDateTime actualPickupDatetime, ZonedDateTime actualReturnDatetime,
                                        String signatureKey) {
        return new Contract(id, contractNumber, reservationId, customerId, vehicleId, scheduledPickup,
                scheduledReturn, status, preInspection, postInspection, actualPickupDatetime, actualReturnDatetime,
                signatureKey);
    }

    public void recordPickup(Inspection preInspection, ZonedDateTime actualPickup) {
        if (this.preInspection != null) {
            throw new InvalidStateTransitionException("Pickup already recorded");
        }
        Objects.requireNonNull(preInspection, "preInspection must not be null");
        Objects.requireNonNull(actualPickup, "actualPickup must not be null");
        this.preInspection = preInspection;
        this.actualPickupDatetime = actualPickup;
        registerEvent(new PickupRecordedEvent(id, vehicleId, actualPickup));
    }

    public boolean recordReturn(Inspection postInspection, ZonedDateTime actualReturn) {
        if (status != ContractStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Cannot record return for a contract in status: " + status);
        }
        if (preInspection == null) {
            throw new InvalidStateTransitionException("Cannot record return before pickup has been recorded");
        }
        Objects.requireNonNull(postInspection, "postInspection must not be null");
        Objects.requireNonNull(actualReturn, "actualReturn must not be null");
        this.postInspection = postInspection;
        this.actualReturnDatetime = actualReturn;
        this.status = ContractStatus.COMPLETED;
        boolean hasDamage = postInspection.checklist().hasDamage();
        registerEvent(new ReturnRecordedEvent(id, vehicleId, actualReturn, hasDamage));
        return hasDamage;
    }

    public void extend(ZonedDateTime newScheduledReturn) {
        if (status != ContractStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Can only extend an active contract");
        }
        Objects.requireNonNull(newScheduledReturn);
        if (!newScheduledReturn.isAfter(scheduledReturn)) {
            throw new DomainException("New return date must be after current scheduled return date");
        }
        this.scheduledReturn = newScheduledReturn;
        this.status = ContractStatus.EXTENDED;
        registerEvent(new ContractExtendedEvent(id, newScheduledReturn));
    }

    public void attachSignature(String signatureKey) {
        if (signatureKey == null || signatureKey.isBlank()) {
            throw new DomainException("signatureKey must not be blank");
        }
        this.signatureKey = signatureKey;
    }

    public ContractId getId() {
        return id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public VehicleId getVehicleId() {
        return vehicleId;
    }

    public ZonedDateTime getScheduledPickup() {
        return scheduledPickup;
    }

    public ZonedDateTime getScheduledReturn() {
        return scheduledReturn;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public Inspection getPreInspection() {
        return preInspection;
    }

    public Inspection getPostInspection() {
        return postInspection;
    }

    public ZonedDateTime getActualPickupDatetime() {
        return actualPickupDatetime;
    }

    public ZonedDateTime getActualReturnDatetime() {
        return actualReturnDatetime;
    }

    public String getSignatureKey() {
        return signatureKey;
    }

    private static String randomContractSuffix() {
        StringBuilder value = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            value.append(CONTRACT_NUMBER_CHARS[RANDOM.nextInt(CONTRACT_NUMBER_CHARS.length)]);
        }
        return value.toString();
    }
}
