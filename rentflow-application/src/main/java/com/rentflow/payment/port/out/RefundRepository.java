package com.rentflow.payment.port.out;

import com.rentflow.payment.Refund;
import com.rentflow.payment.RefundId;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.query.ListRefundsQuery;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface RefundRepository {
    void save(Refund refund);

    Optional<Refund> findById(RefundId id);

    Page<RefundSummary> findAll(ListRefundsQuery query);
}
