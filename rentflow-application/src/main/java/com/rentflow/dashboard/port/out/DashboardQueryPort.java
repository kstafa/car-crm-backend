package com.rentflow.dashboard.port.out;

import com.rentflow.dashboard.model.DailyRevenue;
import com.rentflow.dashboard.model.UpcomingEvent;
import com.rentflow.shared.money.Money;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;

public interface DashboardQueryPort {
    int countActiveRentalsToday();

    int countAvailableVehiclesNow();

    int countPickupsToday();

    int countReturnsToday();

    int countOverdueReturns();

    Money revenueForMonth(YearMonth month);

    List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to);

    List<UpcomingEvent> upcomingEvents(ZonedDateTime from, ZonedDateTime to);
}
