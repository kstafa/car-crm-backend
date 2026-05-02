package com.rentflow.payment.model;

import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.Payment;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.LocalDate;
import java.util.List;

public record InvoiceDetail(
        InvoiceId id,
        String invoiceNumber,
        ContractId contractId,
        CustomerId customerId,
        InvoiceStatus status,
        List<LineItem> lineItems,
        Money totalAmount,
        Money paidAmount,
        Money outstandingAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        List<Payment> payments
) {
}
