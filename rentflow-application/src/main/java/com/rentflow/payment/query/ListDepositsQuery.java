package com.rentflow.payment.query;

import com.rentflow.payment.DepositStatus;
import com.rentflow.shared.id.CustomerId;

public record ListDepositsQuery(DepositStatus status, CustomerId customerId, int page, int size) {
}
