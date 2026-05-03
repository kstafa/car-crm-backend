package com.rentflow.shared.port.out;

import com.rentflow.contract.model.ContractDetail;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.report.model.ReportData;

public interface PdfGeneratorPort {
    byte[] generateInvoice(InvoiceDetail invoice);

    byte[] generateContract(ContractDetail contract);

    byte[] generateReport(ReportData data);
}
