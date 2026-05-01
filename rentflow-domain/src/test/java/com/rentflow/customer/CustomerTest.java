package com.rentflow.customer;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void create_validParams_setsActiveStatus() {
        assertEquals(CustomerStatus.ACTIVE, customer().getStatus());
    }

    @Test
    void isBlacklisted_activeCustomer_returnsFalse() {
        assertFalse(customer().isBlacklisted());
    }

    @Test
    void blacklist_activeCustomer_setsBlacklistedStatus() {
        Customer customer = customer();

        customer.blacklist("fraud");

        assertEquals(CustomerStatus.BLACKLISTED, customer.getStatus());
        assertEquals("fraud", customer.getBlacklistReason());
    }

    @Test
    void blacklist_alreadyBlacklisted_throwsDomainException() {
        Customer customer = customer();
        customer.blacklist("fraud");

        assertThrows(DomainException.class, () -> customer.blacklist("again"));
    }

    @Test
    void reactivate_blacklisted_setsActive() {
        Customer customer = customer();
        customer.blacklist("fraud");

        customer.reactivate();

        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void reactivate_inactive_setsActive() throws Exception {
        Customer customer = customer();
        setStatus(customer, CustomerStatus.INACTIVE);

        customer.reactivate();

        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void reactivate_active_throwsInvalidStateTransition() {
        assertThrows(InvalidStateTransitionException.class, () -> customer().reactivate());
    }

    private static Customer customer() {
        return Customer.create(CustomerId.generate(), "Ada", "Lovelace", "ada@example.com");
    }

    private static void setStatus(Customer customer, CustomerStatus status) throws Exception {
        Field field = Customer.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(customer, status);
    }
}
