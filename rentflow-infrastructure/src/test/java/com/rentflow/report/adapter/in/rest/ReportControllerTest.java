package com.rentflow.report.adapter.in.rest;

import com.rentflow.report.model.FleetUtilizationReport;
import com.rentflow.report.model.RevenueReport;
import com.rentflow.report.port.in.ExportReportUseCase;
import com.rentflow.report.port.in.GetFleetUtilizationUseCase;
import com.rentflow.report.port.in.GetRevenueReportUseCase;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({ReportController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean GetFleetUtilizationUseCase getUtilization;
    @MockBean GetRevenueReportUseCase getRevenue;
    @MockBean ExportReportUseCase exportReport;
    @MockBean ReportMapper mapper;

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getFleetUtilization_validRange_returns200() throws Exception {
        var report = fleetReport();
        when(getUtilization.getUtilization(any())).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(new FleetUtilizationResponse(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), 1, 31, 10, 32.3, List.of()));

        mockMvc.perform(get("/api/v1/reports/fleet-utilization?from=2026-01-01&to=2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(1));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void getFleetUtilization_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/fleet-utilization?from=2026-01-01&to=2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getRevenue_validRange_returns200() throws Exception {
        var report = revenueReport();
        when(getRevenue.getRevenue(any())).thenReturn(report);
        when(mapper.toResponse(report)).thenReturn(new RevenueReportResponse(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), new BigDecimal("10.00"), new BigDecimal("10.00"),
                1, "EUR", List.of()));

        mockMvc.perform(get("/api/v1/reports/revenue?from=2026-01-01&to=2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_EXPORT")
    void exportFleetUtilizationCsv_returns200WithCsvContentType() throws Exception {
        when(exportReport.export(any())).thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/v1/reports/fleet-utilization/export?from=2026-01-01&to=2026-01-31&format=CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_EXPORT")
    void exportFleetUtilizationPdf_returns200WithPdfContentType() throws Exception {
        when(exportReport.export(any())).thenReturn("%PDF".getBytes());

        mockMvc.perform(get("/api/v1/reports/fleet-utilization/export?from=2026-01-01&to=2026-01-31&format=PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_EXPORT")
    void exportRevenueCsv_returns200WithCsvContentType() throws Exception {
        when(exportReport.export(any())).thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/v1/reports/revenue/export?from=2026-01-01&to=2026-01-31&format=CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void exportRevenue_missingExportPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue/export?from=2026-01-01&to=2026-01-31&format=CSV"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getFleetUtilization_toBeforeFrom_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reports/fleet-utilization?from=2026-02-01&to=2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    private static FleetUtilizationReport fleetReport() {
        return new FleetUtilizationReport(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"),
                1, 31, 10, 32.3, List.of());
    }

    private static RevenueReport revenueReport() {
        Money money = new Money(new BigDecimal("10.00"), Currency.getInstance("EUR"));
        return new RevenueReport(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"),
                money, money, 1, List.of());
    }
}
