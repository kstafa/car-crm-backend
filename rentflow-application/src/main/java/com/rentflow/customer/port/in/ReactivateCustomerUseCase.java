package com.rentflow.customer.port.in;

import com.rentflow.customer.command.ReactivateCustomerCommand;

public interface ReactivateCustomerUseCase {
    void reactivate(ReactivateCustomerCommand cmd);
}
