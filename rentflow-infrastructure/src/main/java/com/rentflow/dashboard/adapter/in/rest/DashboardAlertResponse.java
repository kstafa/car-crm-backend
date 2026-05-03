package com.rentflow.dashboard.adapter.in.rest;

public record DashboardAlertResponse(String severity, String category, String message, String entityId) {
}
