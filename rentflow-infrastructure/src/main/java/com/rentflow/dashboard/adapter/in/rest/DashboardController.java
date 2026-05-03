package com.rentflow.dashboard.adapter.in.rest;

import com.rentflow.dashboard.port.in.GetDashboardAlertsUseCase;
import com.rentflow.dashboard.port.in.GetDashboardKpisUseCase;
import com.rentflow.dashboard.port.in.GetRevenueSparklineUseCase;
import com.rentflow.dashboard.port.in.GetUpcomingEventsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboardKpisUseCase getKpis;
    private final GetUpcomingEventsUseCase getUpcoming;
    private final GetDashboardAlertsUseCase getAlerts;
    private final GetRevenueSparklineUseCase getSparkline;
    private final DashboardMapper mapper;

    @GetMapping("/kpis")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<DashboardKpisResponse> kpis(
            @RequestParam(name = "date", required = false) LocalDate date) {
        return ResponseEntity.ok(mapper.toResponse(getKpis.getKpis(date == null ? LocalDate.now() : date)));
    }

    @GetMapping("/upcoming-events")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<UpcomingEventResponse>> upcoming(
            @RequestParam(name = "hours", defaultValue = "48") int hours) {
        return ResponseEntity.ok(getUpcoming.getUpcoming(hours).stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<DashboardAlertResponse>> alerts() {
        return ResponseEntity.ok(getAlerts.getAlerts().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/revenue-sparkline")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<DailyRevenueResponse>> sparkline(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        return ResponseEntity.ok(getSparkline.getSparkline(days).stream().map(mapper::toResponse).toList());
    }
}
