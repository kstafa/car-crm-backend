package com.rentflow.notification.port.in;

import com.rentflow.notification.model.AutomationRuleSummary;

import java.util.List;

public interface ListAutomationRulesUseCase {
    List<AutomationRuleSummary> list();
}
