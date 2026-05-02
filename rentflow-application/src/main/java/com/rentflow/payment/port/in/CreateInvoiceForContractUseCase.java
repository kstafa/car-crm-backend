package com.rentflow.payment.port.in;

import com.rentflow.payment.command.CreateInvoiceForContractCommand;
import com.rentflow.shared.id.InvoiceId;

public interface CreateInvoiceForContractUseCase {
    InvoiceId createForContract(CreateInvoiceForContractCommand command);
}
