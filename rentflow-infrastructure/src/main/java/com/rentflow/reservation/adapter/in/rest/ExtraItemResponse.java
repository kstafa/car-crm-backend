package com.rentflow.reservation.adapter.in.rest;

import java.math.BigDecimal;

public record ExtraItemResponse(String name, BigDecimal unitPrice, int quantity) {
}
