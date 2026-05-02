package com.rentflow.payment.command;

import com.rentflow.payment.DepositId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotNull;

public record ForfeitDepositCommand(
        @NotNull DepositId depositId,
        @NotNull String reason,
        @NotNull StaffId forfeitedBy
) {
}
