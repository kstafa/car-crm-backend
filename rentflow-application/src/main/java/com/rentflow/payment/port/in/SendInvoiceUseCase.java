package com.rentflow.payment.port.in;

import com.rentflow.payment.command.SendInvoiceCommand;

public interface SendInvoiceUseCase {
    void send(SendInvoiceCommand command);
}
