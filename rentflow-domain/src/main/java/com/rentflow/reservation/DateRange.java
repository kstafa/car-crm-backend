package com.rentflow.reservation;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

public record DateRange(ZonedDateTime start, ZonedDateTime end) {

    public DateRange {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be strictly after start");
        }
    }

    public boolean overlapsWith(DateRange other) {
        Objects.requireNonNull(other);
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public long durationInDays() {
        long days = Duration.between(start, end).toDays();
        if (start.plusDays(days).isBefore(end)) {
            days++;
        }
        return Math.max(1, days);
    }
}
