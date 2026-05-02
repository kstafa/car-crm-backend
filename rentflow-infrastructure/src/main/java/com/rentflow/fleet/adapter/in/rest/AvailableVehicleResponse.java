package com.rentflow.fleet.adapter.in.rest;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableVehicleResponse(UUID id, String licensePlate, String brand, String model, int year,
                                       String categoryName, BigDecimal dailyRate, String thumbnailKey) {
}
