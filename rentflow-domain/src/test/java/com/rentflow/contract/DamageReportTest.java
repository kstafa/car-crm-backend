package com.rentflow.contract;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DamageReportTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final VehicleId VEHICLE_ID = VehicleId.generate();
    private static final ContractId CONTRACT_ID = ContractId.generate();
    private static final CustomerId CUSTOMER_ID = CustomerId.generate();

    @Test
    void report_validParams_setsOpenStatusAndRegistersEvent() {
        DamageReport report = report();

        assertEquals(DamageReportStatus.OPEN, report.getStatus());
        assertEquals(DamageReportCreatedEvent.class, report.pullDomainEvents().getFirst().getClass());
    }

    @Test
    void report_blankDescription_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DamageReport.report(VEHICLE_ID, CONTRACT_ID, CUSTOMER_ID,
                " ", DamageSeverity.MINOR, DamageLiability.CUSTOMER, money("100.00")));
    }

    @Test
    void report_nullVehicleId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DamageReport.report(null, CONTRACT_ID, CUSTOMER_ID,
                "Scratch", DamageSeverity.MINOR, DamageLiability.CUSTOMER, money("100.00")));
    }

    @Test
    void addPhoto_validKey_appendsToList() {
        DamageReport report = report();

        report.addPhoto("damage/a.jpg");

        assertEquals(List.of("damage/a.jpg"), report.getPhotoKeys());
    }

    @Test
    void addPhoto_blankKey_throwsDomainException() {
        DamageReport report = report();

        assertThrows(DomainException.class, () -> report.addPhoto(" "));
    }

    @Test
    void settle_openReport_setsSettledWithActualCost() {
        DamageReport report = report();

        report.settle(money("90.00"));

        assertEquals(DamageReportStatus.SETTLED, report.getStatus());
        assertEquals(money("90.00"), report.getActualCost());
    }

    @Test
    void settle_closedReport_throwsInvalidStateTransition() {
        DamageReport report = report();
        report.close();

        assertThrows(InvalidStateTransitionException.class, () -> report.settle(money("90.00")));
    }

    @Test
    void close_settledReport_setsClosed() {
        DamageReport report = report();
        report.settle(money("90.00"));

        report.close();

        assertEquals(DamageReportStatus.CLOSED, report.getStatus());
    }

    @Test
    void close_openReport_setsClosed() {
        DamageReport report = report();

        report.close();

        assertEquals(DamageReportStatus.CLOSED, report.getStatus());
    }

    @Test
    void close_underRepairReport_throwsInvalidStateTransition() {
        DamageReport report = report();
        report.startRepair();

        assertThrows(InvalidStateTransitionException.class, report::close);
    }

    @Test
    void startRepair_openReport_setsUnderRepair() {
        DamageReport report = report();

        report.startRepair();

        assertEquals(DamageReportStatus.UNDER_REPAIR, report.getStatus());
    }

    @Test
    void startRepair_settledReport_throwsInvalidStateTransition() {
        DamageReport report = report();
        report.settle(money("90.00"));

        assertThrows(InvalidStateTransitionException.class, report::startRepair);
    }

    private static DamageReport report() {
        return DamageReport.report(VEHICLE_ID, CONTRACT_ID, CUSTOMER_ID, "Scratch on front bumper",
                DamageSeverity.MINOR, DamageLiability.CUSTOMER, money("100.00"));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
