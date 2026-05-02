package com.rentflow.payment.port.in;

import com.rentflow.payment.command.ProcessRefundCommand;

public interface ProcessRefundUseCase {
    void process(ProcessRefundCommand command);
}
