package com.rentflow.fleet;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class VehicleCategoryTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void create_validParams_setsActiveTrue() {
        VehicleCategory category = validCategory();

        assertTrue(category.isActive());
        assertEquals("Economy", category.getName());
        assertEquals(1, category.pullDomainEvents().size());
    }

    @Test
    void create_blankName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> VehicleCategory.create(" ", "desc", money("49.99"), money("300.00"), rate("0.20")));
    }

    @Test
    void create_negativeDailyRate_throwsDomainException() {
        assertThrows(DomainException.class,
                () -> VehicleCategory.create("Economy", "desc", money("0.00"), money("300.00"), rate("0.20")));
    }

    @Test
    void create_taxRateAboveOne_throwsDomainException() {
        assertThrows(DomainException.class,
                () -> VehicleCategory.create("Economy", "desc", money("49.99"), money("300.00"), rate("1.01")));
    }

    @Test
    void create_taxRateNegative_throwsDomainException() {
        assertThrows(DomainException.class,
                () -> VehicleCategory.create("Economy", "desc", money("49.99"), money("300.00"), rate("-0.01")));
    }

    @Test
    void create_taxRateZero_allowed() {
        VehicleCategory category = VehicleCategory.create("Economy", "desc", money("49.99"), money("300.00"),
                BigDecimal.ZERO);

        assertEquals(0, BigDecimal.ZERO.compareTo(category.getTaxRate()));
    }

    @Test
    void create_taxRateOne_allowed() {
        VehicleCategory category = VehicleCategory.create("Economy", "desc", money("49.99"), money("300.00"),
                BigDecimal.ONE);

        assertEquals(0, BigDecimal.ONE.compareTo(category.getTaxRate()));
    }

    @Test
    void deactivate_activeCategory_setsInactive() {
        VehicleCategory category = validCategory();

        category.deactivate();

        assertFalse(category.isActive());
    }

    @Test
    void updateRates_negativeRate_throwsDomainException() {
        VehicleCategory category = validCategory();

        assertThrows(DomainException.class, () -> category.updateRates(money("0.00"), money("300.00")));
    }

    @Test
    void updateRates_valid_updatesFields() {
        VehicleCategory category = validCategory();

        category.updateRates(money("59.99"), money("350.00"));

        assertEquals(money("59.99"), category.getBaseDailyRate());
        assertEquals(money("350.00"), category.getDepositAmount());
    }

    private static VehicleCategory validCategory() {
        return VehicleCategory.create("Economy", "Small vehicles", money("49.99"), money("300.00"), rate("0.20"));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    private static BigDecimal rate(String amount) {
        return new BigDecimal(amount);
    }
}
