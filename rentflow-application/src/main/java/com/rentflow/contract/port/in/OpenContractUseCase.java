package com.rentflow.contract.port.in;

import com.rentflow.contract.command.OpenContractCommand;
import com.rentflow.shared.id.ContractId;

public interface OpenContractUseCase {
    ContractId open(OpenContractCommand command);
}
