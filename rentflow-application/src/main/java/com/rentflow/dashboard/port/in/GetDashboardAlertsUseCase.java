package com.rentflow.dashboard.port.in;

import com.rentflow.dashboard.model.DashboardAlert;

import java.util.List;

public interface GetDashboardAlertsUseCase {
    List<DashboardAlert> getAlerts();
}
