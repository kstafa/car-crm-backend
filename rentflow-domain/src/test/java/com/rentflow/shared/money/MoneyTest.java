package com.rentflow.shared.money;

import com.rentflow.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void add_sameCurrency_returnsCorrectSum() {
        Money sum = new Money(new BigDecimal("10.00"), EUR).add(new Money(new BigDecimal("2.55"), EUR));

        assertEquals(new BigDecimal("12.55"), sum.amount());
        assertEquals(EUR, sum.currency());
    }

    @Test
    void add_differentCurrencies_throwsIllegalArgumentException() {
        Money eur = new Money(new BigDecimal("10.00"), EUR);
        Money usd = new Money(new BigDecimal("2.00"), USD);

        assertThrows(IllegalArgumentException.class, () -> eur.add(usd));
    }

    @Test
    void subtract_sameCurrency_returnsCorrectDifference() {
        Money result = new Money(new BigDecimal("10.00"), EUR).subtract(new Money(new BigDecimal("2.55"), EUR));

        assertEquals(new BigDecimal("7.45"), result.amount());
    }

    @Test
    void subtract_resultNegative_allowedBecauseMoneyCanBeNegativeAfterSubtract() {
        Money amount = new Money(new BigDecimal("2.00"), EUR);
        Money larger = new Money(new BigDecimal("3.00"), EUR);

        DomainException exception = assertThrows(DomainException.class, () -> amount.subtract(larger));
        assertEquals("Money amount cannot be negative", exception.getMessage());
    }

    @Test
    void multiply_positiveFactorSameCurrency_returnsScaledAmount() {
        Money result = new Money(new BigDecimal("10.005"), EUR).multiply(new BigDecimal("2"));

        assertEquals(new BigDecimal("20.02"), result.amount());
        assertEquals(EUR, result.currency());
    }

    @Test
    void isGreaterThan_largerAmount_returnsTrue() {
        assertTrue(new Money(new BigDecimal("10.00"), EUR).isGreaterThan(new Money(new BigDecimal("9.99"), EUR)));
    }

    @Test
    void isGreaterThan_equalAmount_returnsFalse() {
        assertFalse(new Money(new BigDecimal("10.00"), EUR).isGreaterThan(new Money(new BigDecimal("10.00"), EUR)));
    }

    @Test
    void zero_returnsZeroAmount() {
        Money zero = Money.zero(EUR);

        assertEquals(new BigDecimal("0.00"), zero.amount());
        assertEquals(EUR, zero.currency());
    }

    @Test
    void constructor_negativeAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("-0.01"), EUR));
    }

    @Test
    void constructor_nullAmount_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Money(null, EUR));
    }

    @Test
    void constructor_nullCurrency_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.ONE, null));
    }

    @Test
    void equals_sameAmountAndCurrency_returnsTrue() {
        assertTrue(new Money(new BigDecimal("10.0"), EUR).equals(new Money(new BigDecimal("10.00"), EUR)));
    }

    @Test
    void equals_differentAmount_returnsFalse() {
        assertFalse(new Money(new BigDecimal("10.00"), EUR).equals(new Money(new BigDecimal("10.01"), EUR)));
    }
}
