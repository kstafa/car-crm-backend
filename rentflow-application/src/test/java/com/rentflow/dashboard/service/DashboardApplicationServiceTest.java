package com.rentflow.dashboard.service;

import com.rentflow.dashboard.model.DailyRevenue;
import com.rentflow.dashboard.port.out.DashboardQueryPort;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Mock DashboardQueryPort queryPort;
    DashboardApplicationService service;

    @BeforeEach
    void setUp() {
        service = new DashboardApplicationService(queryPort);
    }

    @Test
    void getKpis_calculatesRevenueChangePercent_positiveGrowth() {
        LocalDate date = LocalDate.of(2026, 5, 3);
        when(queryPort.revenueForMonth(YearMonth.of(2026, 5))).thenReturn(money("150.00"));
        when(queryPort.revenueForMonth(YearMonth.of(2026, 4))).thenReturn(money("100.00"));

        var kpis = service.getKpis(date);

        assertEquals(50.0, kpis.revenueChangePercent());
    }

    @Test
    void getKpis_lastMonthZeroRevenue_changePercentIsZero() {
        LocalDate date = LocalDate.of(2026, 5, 3);
        when(queryPort.revenueForMonth(YearMonth.of(2026, 5))).thenReturn(money("150.00"));
        when(queryPort.revenueForMonth(YearMonth.of(2026, 4))).thenReturn(money("0.00"));

        var kpis = service.getKpis(date);

        assertEquals(0.0, kpis.revenueChangePercent());
    }

    @Test
    void getKpis_delegatesAllCountsToQueryPort() {
        LocalDate date = LocalDate.of(2026, 5, 3);
        when(queryPort.revenueForMonth(any())).thenReturn(money("0.00"));

        service.getKpis(date);

        verify(queryPort).countActiveRentalsToday();
        verify(queryPort).countAvailableVehiclesNow();
        verify(queryPort).countPickupsToday();
        verify(queryPort).countReturnsToday();
        verify(queryPort).countOverdueReturns();
    }

    @Test
    void getAlerts_noOverdue_returnsEmptyList() {
        when(queryPort.countOverdueReturns()).thenReturn(0);

        assertTrue(service.getAlerts().isEmpty());
    }

    @Test
    void getAlerts_overdueExists_returnsCriticalAlert() {
        when(queryPort.countOverdueReturns()).thenReturn(2);

        var alerts = service.getAlerts();

        assertEquals("CRITICAL", alerts.getFirst().severity());
    }

    @Test
    void getSparkline_delegatesWithCorrectDateRange() {
        when(queryPort.dailyRevenue(any(), any())).thenReturn(List.of());

        List<DailyRevenue> result = service.getSparkline(7);

        assertTrue(result.isEmpty());
        verify(queryPort).dailyRevenue(any(LocalDate.class), any(LocalDate.class));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
