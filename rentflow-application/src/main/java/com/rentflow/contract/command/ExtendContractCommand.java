package com.rentflow.contract.command;

import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.StaffId;

import java.time.ZonedDateTime;
import java.util.Objects;

public record ExtendContractCommand(ContractId contractId, ZonedDateTime newScheduledReturn, StaffId extendedBy) {
    public ExtendContractCommand {
        Objects.requireNonNull(contractId);
        Objects.requireNonNull(newScheduledReturn);
    }
}
