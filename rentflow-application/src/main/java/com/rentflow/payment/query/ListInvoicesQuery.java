package com.rentflow.payment.query;

import com.rentflow.payment.InvoiceStatus;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;

import java.time.LocalDate;

public record ListInvoicesQuery(
        InvoiceStatus status,
        CustomerId customerId,
        ContractId contractId,
        LocalDate from,
        LocalDate to,
        int page,
        int size
) {
}
