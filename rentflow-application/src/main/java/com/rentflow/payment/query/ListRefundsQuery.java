package com.rentflow.payment.query;

import com.rentflow.payment.RefundStatus;
import com.rentflow.shared.id.CustomerId;

public record ListRefundsQuery(RefundStatus status, CustomerId customerId, int page, int size) {
}
