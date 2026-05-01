package com.rentflow.shared;

import com.rentflow.shared.id.CustomerId;

public class BlacklistedCustomerException extends DomainException {
    public BlacklistedCustomerException(CustomerId customerId) {
        super("Customer is blacklisted: " + customerId.value());
    }
}
