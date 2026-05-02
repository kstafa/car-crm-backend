package com.rentflow.reservation.adapter.out.persistence;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationExtra;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

@Component
public class ReservationJpaMapper {

    public ReservationJpaEntity toJpa(Reservation domain) {
        var entity = new ReservationJpaEntity();
        entity.id = domain.getId().value();
        entity.reservationNumber = domain.getReservationNumber();
        entity.customerId = domain.getCustomerId().value();
        entity.vehicleId = domain.getVehicleId().value();
        entity.pickupDatetime = domain.getRentalPeriod().start();
        entity.returnDatetime = domain.getRentalPeriod().end();
        entity.status = domain.getStatus();
        entity.baseAmount = domain.getBaseAmount().amount();
        entity.currency = domain.getBaseAmount().currency().getCurrencyCode();
        entity.discountAmount = domain.getDiscountAmount().amount();
        entity.depositAmount = domain.getDepositAmount().amount();
        entity.taxAmount = domain.getTaxAmount().amount();
        entity.notes = domain.getNotes();
        entity.extras = new java.util.ArrayList<>(domain.getExtras().stream().map(this::toJpa).toList());
        return entity;
    }

    public Reservation toDomain(ReservationJpaEntity e) {
        Currency currency = Currency.getInstance(e.currency);
        return Reservation.reconstitute(
                ReservationId.of(e.id),
                e.reservationNumber,
                CustomerId.of(e.customerId),
                VehicleId.of(e.vehicleId),
                new DateRange(normalize(e.pickupDatetime), normalize(e.returnDatetime)),
                e.status,
                new Money(e.baseAmount, currency),
                new Money(e.discountAmount, currency),
                new Money(e.depositAmount, currency),
                new Money(e.taxAmount, currency),
                e.extras.stream().map(extra -> toDomain(extra, currency)).toList(),
                e.notes
        );
    }

    public ReservationSummary toSummary(ReservationJpaEntity e) {
        Currency currency = Currency.getInstance(e.currency);
        Money totalAmount = new Money(e.baseAmount, currency)
                .subtract(new Money(e.discountAmount, currency))
                .add(new Money(e.taxAmount, currency));
        return new ReservationSummary(
                ReservationId.of(e.id),
                e.reservationNumber,
                CustomerId.of(e.customerId),
                VehicleId.of(e.vehicleId),
                normalize(e.pickupDatetime),
                normalize(e.returnDatetime),
                e.status,
                totalAmount
        );
    }

    private ReservationExtraJpaEntity toJpa(ReservationExtra extra) {
        var entity = new ReservationExtraJpaEntity();
        entity.id = UUID.randomUUID();
        entity.name = extra.getName();
        entity.unitPrice = extra.getUnitPrice().amount();
        entity.quantity = extra.getQuantity();
        return entity;
    }

    private ReservationExtra toDomain(ReservationExtraJpaEntity extra, Currency currency) {
        return new ReservationExtra(extra.name, new Money(extra.unitPrice, currency), extra.quantity);
    }

    private java.time.ZonedDateTime normalize(java.time.ZonedDateTime timestamp) {
        return timestamp.withZoneSameInstant(ZoneId.of("UTC"));
    }
}
