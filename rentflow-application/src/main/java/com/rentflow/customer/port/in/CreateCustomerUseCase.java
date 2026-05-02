package com.rentflow.customer.port.in;

import com.rentflow.customer.command.CreateCustomerCommand;
import com.rentflow.shared.id.CustomerId;

public interface CreateCustomerUseCase {
    CustomerId create(CreateCustomerCommand cmd);
}
