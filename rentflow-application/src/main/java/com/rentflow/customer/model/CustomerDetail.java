package com.rentflow.customer.model;

import com.rentflow.customer.CustomerStatus;
import com.rentflow.shared.id.CustomerId;

public record CustomerDetail(CustomerId id, String firstName, String lastName, String email, String phone,
                             CustomerStatus status, String blacklistReason) {
}
