package com.rentflow.customer;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;

import java.util.Objects;

public final class Customer extends AggregateRoot {

    private final CustomerId id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private CustomerStatus status;
    private String blacklistReason;

    private Customer(CustomerId id, String firstName, String lastName, String email, CustomerStatus status,
                     String blacklistReason) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.blacklistReason = blacklistReason;
    }

    public static Customer create(CustomerId id, String firstName, String lastName, String email) {
        return new Customer(
                Objects.requireNonNull(id),
                Objects.requireNonNull(firstName),
                Objects.requireNonNull(lastName),
                Objects.requireNonNull(email),
                CustomerStatus.ACTIVE,
                null
        );
    }

    public boolean isBlacklisted() {
        return status == CustomerStatus.BLACKLISTED;
    }

    public void blacklist(String reason) {
        if (isBlacklisted()) {
            throw new DomainException("Customer is already blacklisted");
        }
        status = CustomerStatus.BLACKLISTED;
        blacklistReason = reason;
    }

    public void reactivate() {
        if (status != CustomerStatus.BLACKLISTED && status != CustomerStatus.INACTIVE) {
            throw new InvalidStateTransitionException("Only blacklisted or inactive customers can be reactivated");
        }
        status = CustomerStatus.ACTIVE;
        blacklistReason = null;
    }

    public CustomerId getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public String getBlacklistReason() {
        return blacklistReason;
    }
}
