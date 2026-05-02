package com.rentflow.reservation;

import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationPricingServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime START = ZonedDateTime.of(2026, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private final ReservationPricingService service = new ReservationPricingService();

    @Test
    void calculateBaseAmount_threeDays_returnsThreeTimesDailyRate() {
        Money result = service.calculateBaseAmount(money("100.00"), new DateRange(START, START.plusDays(3)));

        assertEquals(money("300.00"), result);
    }

    @Test
    void calculateBaseAmount_sameDay_returnsOneDayRate() {
        Money result = service.calculateBaseAmount(money("100.00"), new DateRange(START, START.plusHours(2)));

        assertEquals(money("100.00"), result);
    }

    @Test
    void calculateLateFee_returnedOnTime_returnsZero() {
        assertEquals(Money.zero(EUR), service.calculateLateFee(START, START, money("10.00")));
    }

    @Test
    void calculateLateFee_returnedEarly_returnsZero() {
        assertEquals(Money.zero(EUR), service.calculateLateFee(START, START.minusHours(1), money("10.00")));
    }

    @Test
    void calculateLateFee_twoHoursLate_returnsTwoHourlyRates() {
        assertEquals(money("20.00"), service.calculateLateFee(START, START.plusHours(2), money("10.00")));
    }

    @Test
    void calculateFuelSurcharge_contractedFullReturnedFull_returnsZero() {
        assertEquals(Money.zero(EUR), service.calculateFuelSurcharge(FuelLevel.FULL, FuelLevel.FULL, money("2.00"), 40));
    }

    @Test
    void calculateFuelSurcharge_contractedFullReturnedHalf_chargesForMissingLiters() {
        assertEquals(money("40.00"), service.calculateFuelSurcharge(FuelLevel.FULL, FuelLevel.HALF, money("2.00"), 40));
    }

    @Test
    void calculateFuelSurcharge_returnedMoreThanContracted_returnsZero() {
        assertEquals(Money.zero(EUR), service.calculateFuelSurcharge(FuelLevel.HALF, FuelLevel.FULL, money("2.00"), 40));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
