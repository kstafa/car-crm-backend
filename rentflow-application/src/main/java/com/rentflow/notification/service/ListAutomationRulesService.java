package com.rentflow.notification.service;

import com.rentflow.notification.model.AutomationRuleSummary;
import com.rentflow.notification.port.in.ListAutomationRulesUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ListAutomationRulesService implements ListAutomationRulesUseCase {
    private final NotificationApplicationService service;

    ListAutomationRulesService(NotificationApplicationService service) {
        this.service = service;
    }

    @Override
    public List<AutomationRuleSummary> list() {
        return service.listAutomationRules();
    }
}
