package com.rentflow.payment;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class RefundTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void request_validParams_setsPendingAndRegistersEvent() {
        Refund refund = refund();

        assertEquals(RefundStatus.PENDING, refund.getStatus());
        assertEquals(1, refund.pullDomainEvents().size());
    }

    @Test
    void request_negativeAmount_throwsDomainException() {
        assertThrows(DomainException.class, () -> Refund.request(InvoiceId.generate(), CustomerId.generate(),
                money("-1.00"), RefundReason.OTHER, PaymentMethod.CARD, null));
    }

    @Test
    void approve_pendingRefund_setsApprovedAndRegistersEvent() {
        Refund refund = refund();
        refund.pullDomainEvents();

        refund.approve(StaffId.generate());

        assertEquals(RefundStatus.APPROVED, refund.getStatus());
        assertEquals(1, refund.pullDomainEvents().size());
    }

    @Test
    void approve_alreadyApproved_throwsInvalidStateTransition() {
        Refund refund = refund();
        refund.approve(StaffId.generate());

        assertThrows(InvalidStateTransitionException.class, () -> refund.approve(StaffId.generate()));
    }

    @Test
    void approve_processed_throwsInvalidStateTransition() {
        Refund refund = refund();
        refund.approve(StaffId.generate());
        refund.process(StaffId.generate());

        assertThrows(InvalidStateTransitionException.class, () -> refund.approve(StaffId.generate()));
    }

    @Test
    void process_approvedRefund_setsProcessedAndRegistersEvent() {
        Refund refund = refund();
        refund.approve(StaffId.generate());
        refund.pullDomainEvents();

        refund.process(StaffId.generate());

        assertEquals(RefundStatus.PROCESSED, refund.getStatus());
        assertEquals(1, refund.pullDomainEvents().size());
    }

    @Test
    void process_pendingRefund_throwsInvalidStateTransition() {
        Refund refund = refund();

        assertThrows(InvalidStateTransitionException.class, () -> refund.process(StaffId.generate()));
    }

    @Test
    void reject_pendingRefund_setsRejectedAndRegistersEvent() {
        Refund refund = refund();
        refund.pullDomainEvents();

        refund.reject(StaffId.generate());

        assertEquals(RefundStatus.REJECTED, refund.getStatus());
        assertEquals(1, refund.pullDomainEvents().size());
    }

    @Test
    void reject_approvedRefund_throwsInvalidStateTransition() {
        Refund refund = refund();
        refund.approve(StaffId.generate());

        assertThrows(InvalidStateTransitionException.class, () -> refund.reject(StaffId.generate()));
    }

    private static Refund refund() {
        return Refund.request(InvoiceId.generate(), CustomerId.generate(), money("20.00"), RefundReason.GOODWILL,
                PaymentMethod.CARD, "notes");
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
