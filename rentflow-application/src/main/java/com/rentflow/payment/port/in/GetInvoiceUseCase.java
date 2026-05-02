package com.rentflow.payment.port.in;

import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.shared.id.InvoiceId;

public interface GetInvoiceUseCase {
    InvoiceDetail get(InvoiceId id);
}
