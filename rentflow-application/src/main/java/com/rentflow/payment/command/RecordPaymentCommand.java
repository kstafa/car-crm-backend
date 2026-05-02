package com.rentflow.payment.command;

import com.rentflow.payment.PaymentMethod;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import jakarta.validation.constraints.NotNull;

public record RecordPaymentCommand(
        @NotNull InvoiceId invoiceId,
        @NotNull Money amount,
        @NotNull PaymentMethod method,
        String gatewayReference,
        @NotNull StaffId recordedBy
) {
}
