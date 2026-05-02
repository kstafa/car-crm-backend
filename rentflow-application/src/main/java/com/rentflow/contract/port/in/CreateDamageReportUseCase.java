package com.rentflow.contract.port.in;

import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.command.CreateDamageReportCommand;

public interface CreateDamageReportUseCase {
    DamageReportId create(CreateDamageReportCommand command);
}
