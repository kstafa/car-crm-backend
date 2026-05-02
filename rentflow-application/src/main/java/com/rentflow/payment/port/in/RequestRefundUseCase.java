package com.rentflow.payment.port.in;

import com.rentflow.payment.RefundId;
import com.rentflow.payment.command.RequestRefundCommand;

public interface RequestRefundUseCase {
    RefundId request(RequestRefundCommand command);
}
