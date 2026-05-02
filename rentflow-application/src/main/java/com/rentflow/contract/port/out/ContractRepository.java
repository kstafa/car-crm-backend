package com.rentflow.contract.port.out;

import com.rentflow.contract.Contract;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.query.ListContractsQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.ReservationId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ContractRepository {
    void save(Contract contract);

    Optional<Contract> findById(ContractId id);

    Optional<Contract> findByReservationId(ReservationId reservationId);

    Page<ContractSummary> findAll(ListContractsQuery query);

    List<ContractSummary> findActive();
}
