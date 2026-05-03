package com.rentflow.report.service;

import com.rentflow.report.model.ExportReportCommand;
import com.rentflow.report.model.FleetUtilizationReport;
import com.rentflow.report.model.MonthlyRevenueRow;
import com.rentflow.report.model.ReportData;
import com.rentflow.report.model.ReportPeriodQuery;
import com.rentflow.report.model.RevenueReport;
import com.rentflow.report.model.VehicleUtilizationRow;
import com.rentflow.report.port.in.ExportReportUseCase;
import com.rentflow.report.port.in.GetFleetUtilizationUseCase;
import com.rentflow.report.port.in.GetRevenueReportUseCase;
import com.rentflow.report.port.out.ReportQueryPort;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.port.out.PdfGeneratorPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@Transactional(readOnly = true)
public class ReportApplicationService implements GetFleetUtilizationUseCase, GetRevenueReportUseCase,
        ExportReportUseCase {

    private final ReportQueryPort reportQueryPort;
    private final PdfGeneratorPort pdfGenerator;

    public ReportApplicationService(ReportQueryPort reportQueryPort, PdfGeneratorPort pdfGenerator) {
        this.reportQueryPort = reportQueryPort;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    public FleetUtilizationReport getUtilization(ReportPeriodQuery query) {
        return reportQueryPort.fleetUtilization(query.from(), query.to());
    }

    @Override
    public RevenueReport getRevenue(ReportPeriodQuery query) {
        return reportQueryPort.revenue(query.from(), query.to());
    }

    @Override
    public byte[] export(ExportReportCommand command) {
        return switch (command.reportType()) {
            case "REVENUE" -> exportRevenue(command);
            case "FLEET_UTILIZATION" -> exportFleetUtilization(command);
            default -> throw new DomainException("Unknown report type: " + command.reportType());
        };
    }

    private byte[] exportRevenue(ExportReportCommand command) {
        RevenueReport report = reportQueryPort.revenue(command.from(), command.to());
        return switch (command.format()) {
            case "CSV" -> toCsv(report);
            case "PDF" -> pdfGenerator.generateReport(new ReportData(command.reportType(), report));
            default -> throw new DomainException("Unknown format: " + command.format());
        };
    }

    private byte[] exportFleetUtilization(ExportReportCommand command) {
        FleetUtilizationReport report = reportQueryPort.fleetUtilization(command.from(), command.to());
        return switch (command.format()) {
            case "CSV" -> toCsv(report);
            case "PDF" -> pdfGenerator.generateReport(new ReportData(command.reportType(), report));
            default -> throw new DomainException("Unknown format: " + command.format());
        };
    }

    private byte[] toCsv(RevenueReport report) {
        StringBuilder sb = new StringBuilder("Month,Revenue,Rentals\n");
        for (MonthlyRevenueRow row : report.monthly()) {
            sb.append(row.month()).append(",")
                    .append(row.revenue().amount().toPlainString()).append(",")
                    .append(row.rentals()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toCsv(FleetUtilizationReport report) {
        StringBuilder sb = new StringBuilder("LicensePlate,Brand,Model,RentedDays,UtilizationPercent\n");
        for (VehicleUtilizationRow row : report.rows()) {
            sb.append(row.licensePlate()).append(",")
                    .append(row.brand()).append(",")
                    .append(row.model()).append(",")
                    .append(row.rentedDays()).append(",")
                    .append(String.format(java.util.Locale.ROOT, "%.1f", row.utilizationPercent())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
