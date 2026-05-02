package com.rentflow.contract.port.in;

import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.query.ListContractsQuery;
import org.springframework.data.domain.Page;

public interface ListContractsUseCase {
    Page<ContractSummary> list(ListContractsQuery query);
}
