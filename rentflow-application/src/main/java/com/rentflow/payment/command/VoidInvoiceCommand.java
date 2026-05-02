package com.rentflow.payment.command;

import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotNull;

public record VoidInvoiceCommand(@NotNull InvoiceId invoiceId, @NotNull StaffId voidedBy) {
}
