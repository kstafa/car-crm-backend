package com.rentflow.payment.port.in;

import com.rentflow.shared.id.InvoiceId;

public interface GenerateInvoicePdfUseCase {
    byte[] generate(InvoiceId id);
}
