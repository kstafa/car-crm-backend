package com.rentflow.contract.command;

import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;

import java.util.Objects;

public record OpenContractCommand(ReservationId reservationId, StaffId openedBy) {
    public OpenContractCommand {
        Objects.requireNonNull(reservationId);
    }
}
