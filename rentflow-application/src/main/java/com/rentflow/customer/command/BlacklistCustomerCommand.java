package com.rentflow.customer.command;

import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;

public record BlacklistCustomerCommand(CustomerId customerId, String reason, StaffId performedBy) {
}
