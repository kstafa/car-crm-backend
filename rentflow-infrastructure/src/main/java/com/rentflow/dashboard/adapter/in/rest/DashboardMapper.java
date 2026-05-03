package com.rentflow.dashboard.adapter.in.rest;

import com.rentflow.dashboard.model.DailyRevenue;
import com.rentflow.dashboard.model.DashboardAlert;
import com.rentflow.dashboard.model.DashboardKpis;
import com.rentflow.dashboard.model.UpcomingEvent;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {
    DashboardKpisResponse toResponse(DashboardKpis kpis) {
        return new DashboardKpisResponse(
                kpis.activeRentalsToday(),
                kpis.availableVehiclesNow(),
                kpis.pickupsTodayCount(),
                kpis.returnsTodayCount(),
                kpis.overdueReturnsCount(),
                kpis.revenueThisMonth().amount(),
                kpis.revenueLastMonth().amount(),
                kpis.revenueChangePercent(),
                kpis.revenueThisMonth().currency().getCurrencyCode());
    }

    UpcomingEventResponse toResponse(UpcomingEvent event) {
        return new UpcomingEventResponse(event.type(), event.referenceId(), event.description(), event.scheduledAt());
    }

    DashboardAlertResponse toResponse(DashboardAlert alert) {
        return new DashboardAlertResponse(alert.severity(), alert.category(), alert.message(), alert.entityId());
    }

    DailyRevenueResponse toResponse(DailyRevenue revenue) {
        return new DailyRevenueResponse(revenue.date(), revenue.amount().amount(),
                revenue.amount().currency().getCurrencyCode());
    }
}
