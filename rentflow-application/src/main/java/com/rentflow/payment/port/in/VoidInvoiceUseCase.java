package com.rentflow.payment.port.in;

import com.rentflow.payment.command.VoidInvoiceCommand;

public interface VoidInvoiceUseCase {
    void voidInvoice(VoidInvoiceCommand command);
}
