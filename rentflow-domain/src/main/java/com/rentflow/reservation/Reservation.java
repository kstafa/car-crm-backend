package com.rentflow.reservation;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Reservation extends AggregateRoot {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] RESERVATION_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final ReservationId id;
    private final String reservationNumber;
    private final CustomerId customerId;
    private final VehicleId vehicleId;
    private DateRange rentalPeriod;
    private ReservationStatus status;
    private final Money baseAmount;
    private Money discountAmount;
    private final Money depositAmount;
    private final Money taxAmount;
    private final List<ReservationExtra> extras;

    private Reservation(ReservationId id, String reservationNumber, CustomerId customerId, VehicleId vehicleId,
                        DateRange rentalPeriod, ReservationStatus status, Money baseAmount, Money discountAmount,
                        Money depositAmount, Money taxAmount, List<ReservationExtra> extras) {
        this.id = id;
        this.reservationNumber = reservationNumber;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.rentalPeriod = rentalPeriod;
        this.status = status;
        this.baseAmount = baseAmount;
        this.discountAmount = discountAmount;
        this.depositAmount = depositAmount;
        this.taxAmount = taxAmount;
        this.extras = extras;
    }

    public static Reservation create(CustomerId customerId, VehicleId vehicleId, DateRange rentalPeriod,
                                     Money baseAmount, Money depositAmount, Money taxAmount) {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(rentalPeriod);
        Objects.requireNonNull(baseAmount);
        Objects.requireNonNull(depositAmount);
        Objects.requireNonNull(taxAmount);
        return new Reservation(
                ReservationId.generate(),
                "RES-" + randomReservationSuffix(),
                customerId,
                vehicleId,
                rentalPeriod,
                ReservationStatus.DRAFT,
                baseAmount,
                Money.zero(baseAmount.currency()),
                depositAmount,
                taxAmount,
                new ArrayList<>()
        );
    }

    public void confirm() {
        if (status != ReservationStatus.DRAFT && status != ReservationStatus.PENDING) {
            throw new InvalidStateTransitionException("Reservation can only be confirmed from DRAFT or PENDING");
        }
        status = ReservationStatus.CONFIRMED;
        registerEvent(new ReservationConfirmedEvent(id, customerId, vehicleId, rentalPeriod));
    }

    public void cancel(String reason) {
        if (status == ReservationStatus.COMPLETED || status == ReservationStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Reservation cannot be cancelled from " + status);
        }
        status = ReservationStatus.CANCELLED;
        registerEvent(new ReservationCancelledEvent(id, customerId, reason));
    }

    public void activate() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new InvalidStateTransitionException("Reservation can only be activated from CONFIRMED");
        }
        status = ReservationStatus.ACTIVE;
    }

    public void complete() {
        if (status != ReservationStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Reservation can only be completed from ACTIVE");
        }
        status = ReservationStatus.COMPLETED;
        registerEvent(new ReservationCompletedEvent(id));
    }

    public void extend(ZonedDateTime newReturnDate) {
        Objects.requireNonNull(newReturnDate);
        if (status != ReservationStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Reservation can only be extended while ACTIVE");
        }
        if (!newReturnDate.isAfter(rentalPeriod.end())) {
            throw new DomainException("newReturnDate must be strictly after current return date");
        }
        rentalPeriod = new DateRange(rentalPeriod.start(), newReturnDate);
        registerEvent(new ReservationExtendedEvent(id, rentalPeriod));
    }

    public void applyDiscount(Money discount) {
        Objects.requireNonNull(discount);
        if (discount.isGreaterThan(baseAmount)) {
            throw new DomainException("Discount cannot exceed base amount");
        }
        discountAmount = discount;
    }

    public Money totalAmount() {
        Money total = baseAmount.subtract(discountAmount);
        for (ReservationExtra extra : extras) {
            total = total.add(extra.totalPrice());
        }
        return total.add(taxAmount);
    }

    public ReservationId getId() {
        return id;
    }

    public String getReservationNumber() {
        return reservationNumber;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public VehicleId getVehicleId() {
        return vehicleId;
    }

    public DateRange getRentalPeriod() {
        return rentalPeriod;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Money getBaseAmount() {
        return baseAmount;
    }

    public Money getDiscountAmount() {
        return discountAmount;
    }

    public Money getDepositAmount() {
        return depositAmount;
    }

    public Money getTaxAmount() {
        return taxAmount;
    }

    public List<ReservationExtra> getExtras() {
        return Collections.unmodifiableList(extras);
    }

    private static String randomReservationSuffix() {
        StringBuilder value = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            value.append(RESERVATION_NUMBER_CHARS[RANDOM.nextInt(RESERVATION_NUMBER_CHARS.length)]);
        }
        return value.toString();
    }
}
