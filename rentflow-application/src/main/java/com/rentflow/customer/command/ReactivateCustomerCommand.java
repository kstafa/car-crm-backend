package com.rentflow.customer.command;

import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;

public record ReactivateCustomerCommand(CustomerId customerId, StaffId performedBy) {
}
