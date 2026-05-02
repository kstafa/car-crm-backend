package com.rentflow.payment.model;

import com.rentflow.payment.InvoiceStatus;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.LocalDate;

public record InvoiceSummary(
        InvoiceId id,
        String invoiceNumber,
        ContractId contractId,
        CustomerId customerId,
        InvoiceStatus status,
        Money totalAmount,
        Money paidAmount,
        Money outstandingAmount,
        LocalDate issueDate,
        LocalDate dueDate
) {
}
