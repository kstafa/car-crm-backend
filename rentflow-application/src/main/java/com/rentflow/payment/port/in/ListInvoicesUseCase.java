package com.rentflow.payment.port.in;

import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.query.ListInvoicesQuery;
import org.springframework.data.domain.Page;

public interface ListInvoicesUseCase {
    Page<InvoiceSummary> list(ListInvoicesQuery query);
}
