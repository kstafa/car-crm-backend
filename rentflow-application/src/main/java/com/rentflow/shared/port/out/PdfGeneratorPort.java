package com.rentflow.shared.port.out;

import com.rentflow.contract.model.ContractDetail;
import com.rentflow.payment.model.InvoiceDetail;

public interface PdfGeneratorPort {
    byte[] generateInvoice(InvoiceDetail invoice);

    byte[] generateContract(ContractDetail contract);
}
