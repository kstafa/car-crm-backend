package com.rentflow.reservation;

import com.rentflow.shared.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

public final class ReservationExtra {

    private final String name;
    private final Money unitPrice;
    private final int quantity;

    public ReservationExtra(String name, Money unitPrice, int quantity) {
        this.name = Objects.requireNonNull(name);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        this.quantity = quantity;
    }

    public Money totalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getName() {
        return name;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
