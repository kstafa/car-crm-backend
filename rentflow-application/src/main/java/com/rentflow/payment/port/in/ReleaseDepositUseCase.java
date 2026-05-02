package com.rentflow.payment.port.in;

import com.rentflow.payment.command.ReleaseDepositCommand;

public interface ReleaseDepositUseCase {
    void release(ReleaseDepositCommand command);
}
