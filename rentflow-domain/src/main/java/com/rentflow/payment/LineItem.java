package com.rentflow.payment;

import com.rentflow.shared.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

public record LineItem(String description, InvoiceLineItemType type, Money unitPrice, int quantity) {
    public LineItem {
        Objects.requireNonNull(description);
        Objects.requireNonNull(type);
        Objects.requireNonNull(unitPrice);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    public Money total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
