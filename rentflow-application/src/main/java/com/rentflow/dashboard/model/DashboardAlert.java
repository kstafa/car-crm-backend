package com.rentflow.dashboard.model;

public record DashboardAlert(
        String severity,
        String category,
        String message,
        String entityId) {
}
