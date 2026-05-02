package com.rentflow.reservation;

import java.math.BigDecimal;
import java.util.Objects;

public record DiscountPolicy(
        BigDecimal maxAgentDiscountPercent,
        BigDecimal maxManagerDiscountPercent
) {
    public DiscountPolicy {
        Objects.requireNonNull(maxAgentDiscountPercent);
        Objects.requireNonNull(maxManagerDiscountPercent);
        if (maxAgentDiscountPercent.compareTo(BigDecimal.ZERO) < 0
                || maxAgentDiscountPercent.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("maxAgentDiscountPercent must be between 0 and 1");
        }
        if (maxManagerDiscountPercent.compareTo(BigDecimal.ZERO) < 0
                || maxManagerDiscountPercent.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("maxManagerDiscountPercent must be between 0 and 1");
        }
        if (maxManagerDiscountPercent.compareTo(maxAgentDiscountPercent) < 0) {
            throw new IllegalArgumentException("maxManagerDiscountPercent must be >= maxAgentDiscountPercent");
        }
    }

    public static DiscountPolicy defaultPolicy() {
        return new DiscountPolicy(new BigDecimal("0.10"), new BigDecimal("0.30"));
    }

    public boolean requiresManagerApproval(BigDecimal discountPercent) {
        Objects.requireNonNull(discountPercent);
        return discountPercent.compareTo(maxAgentDiscountPercent) > 0;
    }
}
