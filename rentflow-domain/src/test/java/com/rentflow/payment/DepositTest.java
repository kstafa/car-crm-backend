package com.rentflow.payment;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class DepositTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void hold_validParams_setsHeldStatusAndRegistersEvent() {
        Deposit deposit = deposit();

        assertEquals(DepositStatus.HELD, deposit.getStatus());
        assertEquals(1, deposit.pullDomainEvents().size());
    }

    @Test
    void hold_negativeAmount_throwsDomainException() {
        assertThrows(DomainException.class, () -> Deposit.hold(ContractId.generate(), CustomerId.generate(),
                InvoiceId.generate(), money("-1.00")));
    }

    @Test
    void hold_zeroAmount_throwsDomainException() {
        assertThrows(DomainException.class, () -> Deposit.hold(ContractId.generate(), CustomerId.generate(),
                InvoiceId.generate(), money("0.00")));
    }

    @Test
    void release_heldDeposit_setsReleasedAndRegistersEvent() {
        Deposit deposit = deposit();
        deposit.pullDomainEvents();

        deposit.release("completed");

        assertEquals(DepositStatus.RELEASED, deposit.getStatus());
        assertEquals(1, deposit.pullDomainEvents().size());
    }

    @Test
    void release_alreadyReleased_throwsInvalidStateTransition() {
        Deposit deposit = deposit();
        deposit.release("completed");

        assertThrows(InvalidStateTransitionException.class, () -> deposit.release("again"));
    }

    @Test
    void release_forfeited_throwsInvalidStateTransition() {
        Deposit deposit = deposit();
        deposit.forfeit("damage");

        assertThrows(InvalidStateTransitionException.class, () -> deposit.release("again"));
    }

    @Test
    void forfeit_heldDeposit_setsForfeitedAndRegistersEvent() {
        Deposit deposit = deposit();
        deposit.pullDomainEvents();

        deposit.forfeit("damage");

        assertEquals(DepositStatus.FORFEITED, deposit.getStatus());
        assertEquals(1, deposit.pullDomainEvents().size());
    }

    @Test
    void forfeit_alreadyForfeited_throwsInvalidStateTransition() {
        Deposit deposit = deposit();
        deposit.forfeit("damage");

        assertThrows(InvalidStateTransitionException.class, () -> deposit.forfeit("again"));
    }

    @Test
    void forfeit_released_throwsInvalidStateTransition() {
        Deposit deposit = deposit();
        deposit.release("completed");

        assertThrows(InvalidStateTransitionException.class, () -> deposit.forfeit("damage"));
    }

    private static Deposit deposit() {
        return Deposit.hold(ContractId.generate(), CustomerId.generate(), InvoiceId.generate(), money("300.00"));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
