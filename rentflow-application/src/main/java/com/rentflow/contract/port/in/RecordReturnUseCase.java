package com.rentflow.contract.port.in;

import com.rentflow.contract.command.RecordReturnCommand;
import com.rentflow.contract.model.ReturnSummary;

public interface RecordReturnUseCase {
    ReturnSummary recordReturn(RecordReturnCommand command);
}
