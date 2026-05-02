package com.rentflow.contract.port.in;

import com.rentflow.contract.model.ContractDetail;
import com.rentflow.shared.id.ContractId;

public interface GetContractUseCase {
    ContractDetail get(ContractId id);
}
