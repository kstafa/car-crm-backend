package com.rentflow.contract.port.in;

import com.rentflow.contract.model.ContractSummary;

import java.util.List;

public interface ListActiveContractsUseCase {
    List<ContractSummary> listActive();
}
