package com.rentflow.shared;

import com.rentflow.reservation.DateRange;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    private static final ZonedDateTime START = ZonedDateTime.of(2026, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void overlapsWith_overlapping_returnsTrue() {
        DateRange first = new DateRange(START, START.plusDays(3));
        DateRange second = new DateRange(START.plusDays(2), START.plusDays(4));

        assertTrue(first.overlapsWith(second));
    }

    @Test
    void overlapsWith_adjacent_returnsFalse() {
        DateRange first = new DateRange(START, START.plusDays(3));
        DateRange second = new DateRange(START.plusDays(3), START.plusDays(4));

        assertFalse(first.overlapsWith(second));
    }

    @Test
    void overlapsWith_disjoint_returnsFalse() {
        DateRange first = new DateRange(START, START.plusDays(3));
        DateRange second = new DateRange(START.plusDays(4), START.plusDays(5));

        assertFalse(first.overlapsWith(second));
    }

    @Test
    void overlapsWith_aContainsB_returnsTrue() {
        DateRange first = new DateRange(START, START.plusDays(5));
        DateRange second = new DateRange(START.plusDays(1), START.plusDays(2));

        assertTrue(first.overlapsWith(second));
    }

    @Test
    void constructor_endBeforeStart_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new DateRange(START, START.minusMinutes(1)));
    }

    @Test
    void constructor_endEqualsStart_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new DateRange(START, START));
    }

    @Test
    void durationInDays_threeDayRange_returnsThree() {
        DateRange range = new DateRange(START, START.plusDays(3));

        assertEquals(3, range.durationInDays());
    }

    @Test
    void durationInDays_sameDayRange_returnsOne() {
        DateRange range = new DateRange(START, START.plusHours(2));

        assertEquals(1, range.durationInDays());
    }
}
