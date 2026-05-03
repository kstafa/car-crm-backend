package com.rentflow.notification.port.in;

import com.rentflow.notification.AutomationRuleId;

public interface ToggleAutomationRuleUseCase {
    void toggle(AutomationRuleId id);
}
