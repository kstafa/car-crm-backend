package com.rentflow.payment.port.in;

import com.rentflow.payment.command.ForfeitDepositCommand;

public interface ForfeitDepositUseCase {
    void forfeit(ForfeitDepositCommand command);
}
