package com.rentflow.contract;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContractTest {

    private static final ReservationId RESERVATION_ID = ReservationId.generate();
    private static final CustomerId CUSTOMER_ID = CustomerId.generate();
    private static final VehicleId VEHICLE_ID = VehicleId.generate();
    private static final StaffId STAFF_ID = StaffId.generate();
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final ZonedDateTime RETURN = PICKUP.plusDays(3);

    @Test
    void open_validParams_setsActiveStatusAndRegistersEvent() {
        Contract contract = contract();

        assertEquals(ContractStatus.ACTIVE, contract.getStatus());
        assertEquals(ContractOpenedEvent.class, contract.pullDomainEvents().getFirst().getClass());
    }

    @Test
    void open_generatesContractNumber_withPrefix() {
        Contract contract = contract();

        assertTrue(contract.getContractNumber().startsWith("CON-"));
        assertEquals(12, contract.getContractNumber().length());
    }

    @Test
    void recordPickup_firstCall_storesInspectionAndDatetime() {
        Contract contract = contract();
        Inspection inspection = preInspection();

        contract.recordPickup(inspection, PICKUP.plusMinutes(5));

        assertEquals(inspection, contract.getPreInspection());
        assertEquals(PICKUP.plusMinutes(5), contract.getActualPickupDatetime());
    }

    @Test
    void recordPickup_secondCall_throwsInvalidStateTransition() {
        Contract contract = contract();
        contract.recordPickup(preInspection(), PICKUP);

        assertThrows(InvalidStateTransitionException.class, () -> contract.recordPickup(preInspection(), PICKUP));
    }

    @Test
    void recordPickup_nullInspection_throwsNullPointerException() {
        Contract contract = contract();

        assertThrows(NullPointerException.class, () -> contract.recordPickup(null, PICKUP));
    }

    @Test
    void recordReturn_afterPickup_setsCompletedAndRegistersEvent() {
        Contract contract = pickedUpContract();
        contract.pullDomainEvents();

        contract.recordReturn(postInspection(InspectionChecklist.allOk()), RETURN);

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertEquals(RETURN, contract.getActualReturnDatetime());
        assertEquals(ReturnRecordedEvent.class, contract.pullDomainEvents().getFirst().getClass());
    }

    @Test
    void recordReturn_beforePickup_throwsInvalidStateTransition() {
        Contract contract = contract();

        assertThrows(InvalidStateTransitionException.class,
                () -> contract.recordReturn(postInspection(InspectionChecklist.allOk()), RETURN));
    }

    @Test
    void recordReturn_contractNotActive_throwsInvalidStateTransition() {
        Contract contract = pickedUpContract();
        contract.extend(RETURN.plusDays(1));

        assertThrows(InvalidStateTransitionException.class,
                () -> contract.recordReturn(postInspection(InspectionChecklist.allOk()), RETURN));
    }

    @Test
    void recordReturn_withDamage_returnsTrueAndEventHasDamageFlagTrue() {
        Contract contract = pickedUpContract();
        contract.pullDomainEvents();

        boolean result = contract.recordReturn(postInspection(damagedChecklist()), RETURN);

        ReturnRecordedEvent event = (ReturnRecordedEvent) contract.pullDomainEvents().getFirst();
        assertTrue(result);
        assertTrue(event.hasDamage());
    }

    @Test
    void recordReturn_withoutDamage_returnsFalseAndEventHasDamageFlagFalse() {
        Contract contract = pickedUpContract();
        contract.pullDomainEvents();

        boolean result = contract.recordReturn(postInspection(InspectionChecklist.allOk()), RETURN);

        ReturnRecordedEvent event = (ReturnRecordedEvent) contract.pullDomainEvents().getFirst();
        assertFalse(result);
        assertFalse(event.hasDamage());
    }

    @Test
    void extend_activeContract_updatesScheduledReturnAndRegistersEvent() {
        Contract contract = contract();
        contract.pullDomainEvents();

        contract.extend(RETURN.plusDays(2));

        assertEquals(RETURN.plusDays(2), contract.getScheduledReturn());
        assertEquals(ContractExtendedEvent.class, contract.pullDomainEvents().getFirst().getClass());
    }

    @Test
    void extend_newDateBeforeCurrent_throwsDomainException() {
        Contract contract = contract();

        assertThrows(DomainException.class, () -> contract.extend(RETURN.minusHours(1)));
    }

    @Test
    void extend_completedContract_throwsInvalidStateTransition() {
        Contract contract = pickedUpContract();
        contract.recordReturn(postInspection(InspectionChecklist.allOk()), RETURN);

        assertThrows(InvalidStateTransitionException.class, () -> contract.extend(RETURN.plusDays(1)));
    }

    @Test
    void attachSignature_validKey_storesKey() {
        Contract contract = contract();

        contract.attachSignature("contracts/signature.pdf");

        assertEquals("contracts/signature.pdf", contract.getSignatureKey());
    }

    @Test
    void attachSignature_blankKey_throwsDomainException() {
        Contract contract = contract();

        assertThrows(DomainException.class, () -> contract.attachSignature(" "));
    }

    private static Contract pickedUpContract() {
        Contract contract = contract();
        contract.recordPickup(preInspection(), PICKUP);
        return contract;
    }

    private static Contract contract() {
        return Contract.open(RESERVATION_ID, CUSTOMER_ID, VEHICLE_ID, PICKUP, RETURN);
    }

    private static Inspection preInspection() {
        return new Inspection(Inspection.InspectionType.PRE, InspectionChecklist.allOk(), FuelLevel.FULL, 1000,
                List.of("contracts/pre.jpg"), Instant.now(), STAFF_ID);
    }

    private static Inspection postInspection(InspectionChecklist checklist) {
        return new Inspection(Inspection.InspectionType.POST, checklist, FuelLevel.FULL, 1200, List.of(),
                Instant.now(), STAFF_ID);
    }

    private static InspectionChecklist damagedChecklist() {
        return new InspectionChecklist(false, true, true, true, true, true, true, true, "front dent");
    }
}
