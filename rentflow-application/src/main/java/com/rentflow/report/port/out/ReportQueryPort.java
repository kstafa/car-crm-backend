package com.rentflow.report.port.out;

import com.rentflow.report.model.FleetUtilizationReport;
import com.rentflow.report.model.RevenueReport;

import java.time.LocalDate;

public interface ReportQueryPort {
    FleetUtilizationReport fleetUtilization(LocalDate from, LocalDate to);

    RevenueReport revenue(LocalDate from, LocalDate to);
}
