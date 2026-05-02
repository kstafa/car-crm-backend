package com.rentflow.reservation.port.in;

import com.rentflow.reservation.model.CalendarEntry;
import com.rentflow.reservation.query.GetCalendarQuery;

import java.util.List;

public interface GetReservationCalendarUseCase {
    List<CalendarEntry> getCalendar(GetCalendarQuery query);
}
