package com.rentflow.dashboard.port.in;

import com.rentflow.dashboard.model.DashboardKpis;

import java.time.LocalDate;

public interface GetDashboardKpisUseCase {
    DashboardKpis getKpis(LocalDate date);
}
