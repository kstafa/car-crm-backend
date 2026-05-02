package com.rentflow.payment.port.out;

import com.rentflow.payment.Deposit;
import com.rentflow.payment.DepositId;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.query.ListDepositsQuery;
import com.rentflow.shared.id.ContractId;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface DepositRepository {
    void save(Deposit deposit);

    Optional<Deposit> findById(DepositId id);

    Optional<Deposit> findByContractId(ContractId contractId);

    Page<DepositSummary> findAll(ListDepositsQuery query);
}
