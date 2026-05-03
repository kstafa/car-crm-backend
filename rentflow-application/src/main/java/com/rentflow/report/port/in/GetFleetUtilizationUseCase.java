package com.rentflow.report.port.in;

import com.rentflow.report.model.FleetUtilizationReport;
import com.rentflow.report.model.ReportPeriodQuery;

public interface GetFleetUtilizationUseCase {
    FleetUtilizationReport getUtilization(ReportPeriodQuery query);
}
