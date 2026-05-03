package com.rentflow.dashboard.service;

import com.rentflow.dashboard.model.DailyRevenue;
import com.rentflow.dashboard.model.DashboardAlert;
import com.rentflow.dashboard.model.DashboardKpis;
import com.rentflow.dashboard.model.UpcomingEvent;
import com.rentflow.dashboard.port.in.GetDashboardAlertsUseCase;
import com.rentflow.dashboard.port.in.GetDashboardKpisUseCase;
import com.rentflow.dashboard.port.in.GetRevenueSparklineUseCase;
import com.rentflow.dashboard.port.in.GetUpcomingEventsUseCase;
import com.rentflow.dashboard.port.out.DashboardQueryPort;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardApplicationService implements GetDashboardKpisUseCase, GetUpcomingEventsUseCase,
        GetDashboardAlertsUseCase, GetRevenueSparklineUseCase {

    private final DashboardQueryPort queryPort;

    public DashboardApplicationService(DashboardQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public DashboardKpis getKpis(LocalDate date) {
        Money thisMonth = queryPort.revenueForMonth(YearMonth.from(date));
        Money lastMonth = queryPort.revenueForMonth(YearMonth.from(date).minusMonths(1));
        double changePercent = lastMonth.amount().compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : thisMonth.amount()
                .subtract(lastMonth.amount())
                .divide(lastMonth.amount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        return new DashboardKpis(
                queryPort.countActiveRentalsToday(),
                queryPort.countAvailableVehiclesNow(),
                queryPort.countPickupsToday(),
                queryPort.countReturnsToday(),
                queryPort.countOverdueReturns(),
                thisMonth,
                lastMonth,
                changePercent);
    }

    @Override
    public List<UpcomingEvent> getUpcoming(int withinHours) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return queryPort.upcomingEvents(now, now.plusHours(withinHours));
    }

    @Override
    public List<DashboardAlert> getAlerts() {
        List<DashboardAlert> alerts = new ArrayList<>();
        int overdue = queryPort.countOverdueReturns();
        if (overdue > 0) {
            alerts.add(new DashboardAlert("CRITICAL", "OVERDUE",
                    overdue + " vehicle(s) not returned on time", null));
        }
        return alerts;
    }

    @Override
    public List<DailyRevenue> getSparkline(int lastDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lastDays - 1L);
        return queryPort.dailyRevenue(from, to);
    }
}
