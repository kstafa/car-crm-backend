package com.rentflow.notification.port.in;

import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.command.CreateAutomationRuleCommand;

public interface CreateAutomationRuleUseCase {
    AutomationRuleId create(CreateAutomationRuleCommand command);
}
