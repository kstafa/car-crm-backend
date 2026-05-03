package com.rentflow.report.adapter.in.rest;

import com.rentflow.report.model.ExportReportCommand;
import com.rentflow.report.model.ReportPeriodQuery;
import com.rentflow.report.port.in.ExportReportUseCase;
import com.rentflow.report.port.in.GetFleetUtilizationUseCase;
import com.rentflow.report.port.in.GetRevenueReportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GetFleetUtilizationUseCase getUtilization;
    private final GetRevenueReportUseCase getRevenue;
    private final ExportReportUseCase exportReport;
    private final ReportMapper mapper;

    @GetMapping("/fleet-utilization")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<FleetUtilizationResponse> fleetUtilization(
            @RequestParam("from") LocalDate from, @RequestParam("to") LocalDate to) {
        return ResponseEntity.ok(mapper.toResponse(getUtilization.getUtilization(new ReportPeriodQuery(from, to))));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<RevenueReportResponse> revenue(
            @RequestParam("from") LocalDate from, @RequestParam("to") LocalDate to) {
        return ResponseEntity.ok(mapper.toResponse(getRevenue.getRevenue(new ReportPeriodQuery(from, to))));
    }

    @GetMapping("/fleet-utilization/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportFleetUtilization(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to,
            @RequestParam(name = "format", defaultValue = "CSV") String format) {
        byte[] data = exportReport.export(new ExportReportCommand("FLEET_UTILIZATION", from, to, format));
        return buildExportResponse(data, format, "fleet-utilization");
    }

    @GetMapping("/revenue/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportRevenue(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to,
            @RequestParam(name = "format", defaultValue = "CSV") String format) {
        byte[] data = exportReport.export(new ExportReportCommand("REVENUE", from, to, format));
        return buildExportResponse(data, format, "revenue");
    }

    private ResponseEntity<byte[]> buildExportResponse(byte[] data, String format, String filename) {
        boolean pdf = "PDF".equalsIgnoreCase(format);
        MediaType mediaType = pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv");
        String ext = pdf ? "pdf" : "csv";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "." + ext + "\"")
                .body(data);
    }
}
