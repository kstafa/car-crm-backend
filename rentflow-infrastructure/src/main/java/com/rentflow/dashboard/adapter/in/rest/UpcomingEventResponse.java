package com.rentflow.dashboard.adapter.in.rest;

import java.time.ZonedDateTime;

public record UpcomingEventResponse(String type, String referenceId, String description, ZonedDateTime scheduledAt) {
}
