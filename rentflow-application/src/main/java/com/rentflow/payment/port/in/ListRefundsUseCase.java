package com.rentflow.payment.port.in;

import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.query.ListRefundsQuery;
import org.springframework.data.domain.Page;

public interface ListRefundsUseCase {
    Page<RefundSummary> list(ListRefundsQuery query);
}
