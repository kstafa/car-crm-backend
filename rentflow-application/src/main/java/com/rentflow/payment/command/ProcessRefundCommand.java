package com.rentflow.payment.command;

import com.rentflow.payment.RefundId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotNull;

public record ProcessRefundCommand(@NotNull RefundId refundId, @NotNull StaffId processedBy) {
}
