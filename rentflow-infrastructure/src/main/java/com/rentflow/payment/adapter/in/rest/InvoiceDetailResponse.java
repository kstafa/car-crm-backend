package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailResponse(
        UUID id,
        String invoiceNumber,
        UUID contractId,
        UUID customerId,
        String status,
        List<LineItemResponse> lineItems,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        String currency,
        LocalDate issueDate,
        LocalDate dueDate,
        List<PaymentResponse> payments
) {
}
