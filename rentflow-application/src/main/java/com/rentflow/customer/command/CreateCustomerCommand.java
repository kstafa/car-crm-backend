package com.rentflow.customer.command;

import com.rentflow.shared.id.StaffId;

public record CreateCustomerCommand(String firstName, String lastName, String email, String phone,
                                    StaffId createdBy) {
    public CreateCustomerCommand {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
    }
}
