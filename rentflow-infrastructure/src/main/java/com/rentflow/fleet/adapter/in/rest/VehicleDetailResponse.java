package com.rentflow.fleet.adapter.in.rest;

import java.util.List;
import java.util.UUID;

public record VehicleDetailResponse(UUID id, String licensePlate, String brand, String model, int year, String status,
                                    UUID categoryId, String categoryName, int currentMileage, boolean active,
                                    String description, List<String> photoKeys) {
}
