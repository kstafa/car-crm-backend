package com.rentflow.payment.port.in;

import com.rentflow.payment.command.ApproveRefundCommand;

public interface ApproveRefundUseCase {
    void approve(ApproveRefundCommand command);
}
