package com.rentflow.payment.port.in;

import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.query.ListDepositsQuery;
import org.springframework.data.domain.Page;

public interface ListDepositsUseCase {
    Page<DepositSummary> list(ListDepositsQuery query);
}
