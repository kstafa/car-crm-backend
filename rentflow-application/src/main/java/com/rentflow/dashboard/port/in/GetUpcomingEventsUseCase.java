package com.rentflow.dashboard.port.in;

import com.rentflow.dashboard.model.UpcomingEvent;

import java.util.List;

public interface GetUpcomingEventsUseCase {
    List<UpcomingEvent> getUpcoming(int withinHours);
}
