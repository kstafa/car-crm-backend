package com.rentflow.report.port.in;

import com.rentflow.report.model.ReportPeriodQuery;
import com.rentflow.report.model.RevenueReport;

public interface GetRevenueReportUseCase {
    RevenueReport getRevenue(ReportPeriodQuery query);
}
