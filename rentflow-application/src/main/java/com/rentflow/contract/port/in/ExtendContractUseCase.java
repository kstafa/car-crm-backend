package com.rentflow.contract.port.in;

import com.rentflow.contract.command.ExtendContractCommand;

public interface ExtendContractUseCase {
    void extend(ExtendContractCommand command);
}
