package com.rentflow.payment.port.in;

import com.rentflow.payment.command.RecordPaymentCommand;

public interface RecordPaymentUseCase {
    void record(RecordPaymentCommand command);
}
