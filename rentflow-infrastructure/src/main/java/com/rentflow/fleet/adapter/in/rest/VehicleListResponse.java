package com.rentflow.fleet.adapter.in.rest;

import java.util.UUID;

public record VehicleListResponse(UUID id, String licensePlate, String brand, String model, int year, String status,
                                  String categoryName, int currentMileage, String thumbnailKey) {
}
