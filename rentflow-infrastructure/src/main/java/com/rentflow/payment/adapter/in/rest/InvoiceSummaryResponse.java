package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
        UUID id,
        String invoiceNumber,
        UUID contractId,
        UUID customerId,
        String status,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        String currency,
        LocalDate issueDate,
        LocalDate dueDate
) {
}
