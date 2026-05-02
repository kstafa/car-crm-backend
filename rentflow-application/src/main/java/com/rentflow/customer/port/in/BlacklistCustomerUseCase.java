package com.rentflow.customer.port.in;

import com.rentflow.customer.command.BlacklistCustomerCommand;

public interface BlacklistCustomerUseCase {
    void blacklist(BlacklistCustomerCommand cmd);
}
