package com.rentflow.report.adapter.in.rest;

import com.rentflow.report.model.FleetUtilizationReport;
import com.rentflow.report.model.RevenueReport;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {
    FleetUtilizationResponse toResponse(FleetUtilizationReport report) {
        return new FleetUtilizationResponse(report.from(), report.to(), report.totalVehicles(), report.totalDays(),
                report.rentedDays(), report.utilizationPercent(),
                report.rows().stream()
                        .map(row -> new VehicleUtilizationRowResponse(row.vehicleId().value(), row.licensePlate(),
                                row.brand(), row.model(), row.rentedDays(), row.utilizationPercent()))
                        .toList());
    }

    RevenueReportResponse toResponse(RevenueReport report) {
        return new RevenueReportResponse(report.from(), report.to(), report.totalRevenue().amount(),
                report.averagePerRental().amount(), report.completedRentals(),
                report.totalRevenue().currency().getCurrencyCode(),
                report.monthly().stream()
                        .map(row -> new MonthlyRevenueRowResponse(row.month().toString(), row.revenue().amount(),
                                row.rentals(), row.revenue().currency().getCurrencyCode()))
                        .toList());
    }
}
