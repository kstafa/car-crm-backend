package com.rentflow.payment.command;

import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.RefundReason;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import jakarta.validation.constraints.NotNull;

public record RequestRefundCommand(
        @NotNull InvoiceId invoiceId,
        @NotNull Money amount,
        @NotNull RefundReason reason,
        @NotNull PaymentMethod method,
        String notes,
        @NotNull StaffId requestedBy
) {
}
