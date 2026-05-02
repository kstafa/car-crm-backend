package com.rentflow.reservation.port.in;

import com.rentflow.reservation.model.ConflictSummary;

import java.util.List;

public interface GetReservationConflictsUseCase {
    List<ConflictSummary> getConflicts();
}
