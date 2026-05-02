package com.rentflow.payment.adapter.in.rest;

import java.math.BigDecimal;

public record LineItemResponse(
        String description,
        String type,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal total,
        String currency
) {
}
