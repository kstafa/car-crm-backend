package com.rentflow.contract.port.in;

import com.rentflow.contract.command.RecordPickupCommand;

public interface RecordPickupUseCase {
    void recordPickup(RecordPickupCommand command);
}
