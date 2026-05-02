package com.rentflow.fleet.model;

import java.time.ZonedDateTime;

public record OccupiedPeriod(ZonedDateTime from, ZonedDateTime to, String type, String referenceId) {
}
