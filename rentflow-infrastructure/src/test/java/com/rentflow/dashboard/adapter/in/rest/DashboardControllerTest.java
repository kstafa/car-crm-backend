package com.rentflow.dashboard.adapter.in.rest;

import com.rentflow.dashboard.model.DailyRevenue;
import com.rentflow.dashboard.model.DashboardAlert;
import com.rentflow.dashboard.model.DashboardKpis;
import com.rentflow.dashboard.model.UpcomingEvent;
import com.rentflow.dashboard.port.in.GetDashboardAlertsUseCase;
import com.rentflow.dashboard.port.in.GetDashboardKpisUseCase;
import com.rentflow.dashboard.port.in.GetRevenueSparklineUseCase;
import com.rentflow.dashboard.port.in.GetUpcomingEventsUseCase;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({DashboardController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean GetDashboardKpisUseCase getKpis;
    @MockBean GetUpcomingEventsUseCase getUpcoming;
    @MockBean GetDashboardAlertsUseCase getAlerts;
    @MockBean GetRevenueSparklineUseCase getSparkline;
    @MockBean DashboardMapper mapper;

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getKpis_authenticated_returns200WithAllFields() throws Exception {
        DashboardKpis kpis = new DashboardKpis(1, 2, 3, 4, 5, money("10.00"), money("8.00"), 25.0);
        when(getKpis.getKpis(any())).thenReturn(kpis);
        when(mapper.toResponse(kpis)).thenReturn(new DashboardKpisResponse(1, 2, 3, 4, 5,
                new BigDecimal("10.00"), new BigDecimal("8.00"), 25.0, "EUR"));

        mockMvc.perform(get("/api/v1/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRentalsToday").value(1))
                .andExpect(jsonPath("$.availableVehiclesNow").value(2))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void getKpis_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/kpis")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void getKpis_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/kpis")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getUpcomingEvents_defaultHours_returns200() throws Exception {
        UpcomingEvent event = event();
        when(getUpcoming.getUpcoming(48)).thenReturn(List.of(event));
        when(mapper.toResponse(event)).thenReturn(new UpcomingEventResponse("PICKUP", "1", "Pickup", event.scheduledAt()));

        mockMvc.perform(get("/api/v1/dashboard/upcoming-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PICKUP"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getUpcomingEvents_customHours_usesParameter() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/upcoming-events?hours=12")).andExpect(status().isOk());

        verify(getUpcoming).getUpcoming(12);
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getAlerts_returns200WithList() throws Exception {
        DashboardAlert alert = new DashboardAlert("CRITICAL", "OVERDUE", "Late", null);
        when(getAlerts.getAlerts()).thenReturn(List.of(alert));
        when(mapper.toResponse(alert)).thenReturn(new DashboardAlertResponse("CRITICAL", "OVERDUE", "Late", null));

        mockMvc.perform(get("/api/v1/dashboard/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getRevenueSparkline_defaultDays_returns200() throws Exception {
        DailyRevenue revenue = new DailyRevenue(LocalDate.now(), money("12.00"));
        when(getSparkline.getSparkline(30)).thenReturn(List.of(revenue));
        when(mapper.toResponse(revenue)).thenReturn(new DailyRevenueResponse(revenue.date(),
                revenue.amount().amount(), "EUR"));

        mockMvc.perform(get("/api/v1/dashboard/revenue-sparkline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getRevenueSparkline_customDays_usesParameter() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/revenue-sparkline?days=7")).andExpect(status().isOk());

        verify(getSparkline).getSparkline(7);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("EUR"));
    }

    private static UpcomingEvent event() {
        return new UpcomingEvent("PICKUP", "1", "Pickup", ZonedDateTime.now(ZoneOffset.UTC));
    }
}
