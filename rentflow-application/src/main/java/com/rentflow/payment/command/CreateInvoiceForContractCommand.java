package com.rentflow.payment.command;

import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.money.Money;
import jakarta.validation.constraints.NotNull;

import java.util.Currency;

public record CreateInvoiceForContractCommand(
        @NotNull ContractId contractId,
        @NotNull CustomerId customerId,
        @NotNull ReservationId reservationId,
        @NotNull Money rentalBaseAmount,
        @NotNull Money discountAmount,
        @NotNull Money taxAmount,
        @NotNull Money lateFee,
        @NotNull Money fuelSurcharge,
        @NotNull Money depositAmount,
        @NotNull Currency currency,
        int rentalDays
) {
}
