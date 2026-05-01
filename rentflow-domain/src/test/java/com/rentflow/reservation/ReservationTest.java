package com.rentflow.reservation;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final CustomerId CUSTOMER_ID = CustomerId.generate();
    private static final VehicleId VEHICLE_ID = VehicleId.generate();
    private static final ZonedDateTime START = ZonedDateTime.of(2026, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void create_validParams_returnsReservationWithDraftStatus() {
        Reservation reservation = reservation();

        assertEquals(ReservationStatus.DRAFT, reservation.getStatus());
    }

    @Test
    void create_generatesNonNullId() {
        assertNotNull(reservation().getId());
    }

    @Test
    void create_generatesReservationNumberWithPrefix() {
        String number = reservation().getReservationNumber();

        assertTrue(number.startsWith("RES-"));
        assertEquals(12, number.length());
    }

    @Test
    void confirm_fromDraft_setsConfirmedAndRegistersEvent() {
        Reservation reservation = reservation();

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(ReservationConfirmedEvent.class, reservation.pullDomainEvents().get(0).getClass());
    }

    @Test
    void confirm_fromPending_setsConfirmedAndRegistersEvent() throws Exception {
        Reservation reservation = reservation();
        setStatus(reservation, ReservationStatus.PENDING);

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(ReservationConfirmedEvent.class, reservation.pullDomainEvents().get(0).getClass());
    }

    @Test
    void confirm_fromConfirmed_throwsInvalidStateTransition() {
        Reservation reservation = confirmedReservation();

        assertThrows(InvalidStateTransitionException.class, reservation::confirm);
    }

    @Test
    void confirm_fromActive_throwsInvalidStateTransition() {
        Reservation reservation = activeReservation();

        assertThrows(InvalidStateTransitionException.class, reservation::confirm);
    }

    @Test
    void confirm_fromCancelled_throwsInvalidStateTransition() {
        Reservation reservation = reservation();
        reservation.cancel("customer request");

        assertThrows(InvalidStateTransitionException.class, reservation::confirm);
    }

    @Test
    void confirm_fromCompleted_throwsInvalidStateTransition() {
        Reservation reservation = activeReservation();
        reservation.complete();

        assertThrows(InvalidStateTransitionException.class, reservation::confirm);
    }

    @Test
    void cancel_fromDraft_setsCancelledAndRegistersEvent() {
        Reservation reservation = reservation();

        reservation.cancel("customer request");

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertTrue(reservation.pullDomainEvents().stream().anyMatch(ReservationCancelledEvent.class::isInstance));
    }

    @Test
    void cancel_fromConfirmed_setsCancelledAndRegistersEvent() {
        Reservation reservation = confirmedReservation();
        reservation.pullDomainEvents();

        reservation.cancel("customer request");

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(ReservationCancelledEvent.class, reservation.pullDomainEvents().get(0).getClass());
    }

    @Test
    void cancel_fromCompleted_throwsInvalidStateTransition() {
        Reservation reservation = activeReservation();
        reservation.complete();

        assertThrows(InvalidStateTransitionException.class, () -> reservation.cancel("too late"));
    }

    @Test
    void cancel_fromCancelled_throwsInvalidStateTransition() {
        Reservation reservation = reservation();
        reservation.cancel("customer request");

        assertThrows(InvalidStateTransitionException.class, () -> reservation.cancel("again"));
    }

    @Test
    void activate_fromConfirmed_setsActive() {
        Reservation reservation = confirmedReservation();

        reservation.activate();

        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    void activate_fromDraft_throwsInvalidStateTransition() {
        assertThrows(InvalidStateTransitionException.class, () -> reservation().activate());
    }

    @Test
    void complete_fromActive_setsCompletedAndRegistersEvent() {
        Reservation reservation = activeReservation();
        reservation.pullDomainEvents();

        reservation.complete();

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        assertEquals(ReservationCompletedEvent.class, reservation.pullDomainEvents().get(0).getClass());
    }

    @Test
    void complete_fromConfirmed_throwsInvalidStateTransition() {
        assertThrows(InvalidStateTransitionException.class, () -> confirmedReservation().complete());
    }

    @Test
    void extend_fromActive_updatesReturnDateAndRegistersEvent() {
        Reservation reservation = activeReservation();
        reservation.pullDomainEvents();
        ZonedDateTime newReturnDate = START.plusDays(5);

        reservation.extend(newReturnDate);

        assertEquals(newReturnDate, reservation.getRentalPeriod().end());
        assertEquals(ReservationExtendedEvent.class, reservation.pullDomainEvents().get(0).getClass());
    }

    @Test
    void extend_newDateBeforeCurrent_throwsDomainException() {
        Reservation reservation = activeReservation();

        assertThrows(DomainException.class, () -> reservation.extend(START.plusDays(1)));
    }

    @Test
    void extend_newDateEqualToCurrent_throwsDomainException() {
        Reservation reservation = activeReservation();

        assertThrows(DomainException.class, () -> reservation.extend(reservation.getRentalPeriod().end()));
    }

    @Test
    void extend_fromConfirmed_throwsInvalidStateTransition() {
        assertThrows(InvalidStateTransitionException.class, () -> confirmedReservation().extend(START.plusDays(4)));
    }

    @Test
    void applyDiscount_belowBase_setsDiscount() {
        Reservation reservation = reservation();
        Money discount = money("10.00");

        reservation.applyDiscount(discount);

        assertEquals(discount, reservation.getDiscountAmount());
    }

    @Test
    void applyDiscount_equalsBase_allowed() {
        Reservation reservation = reservation();
        Money discount = money("100.00");

        reservation.applyDiscount(discount);

        assertEquals(discount, reservation.getDiscountAmount());
    }

    @Test
    void applyDiscount_exceedsBase_throwsDomainException() {
        Reservation reservation = reservation();

        assertThrows(DomainException.class, () -> reservation.applyDiscount(money("100.01")));
    }

    @Test
    void totalAmount_withExtrasAndDiscount_calculatesCorrectly() throws Exception {
        Reservation reservation = reservation();
        reservation.applyDiscount(money("10.00"));
        setExtras(reservation, List.of(
                new ReservationExtra("child seat", money("5.00"), 2),
                new ReservationExtra("gps", money("3.50"), 1)
        ));

        assertEquals(money("113.50"), reservation.totalAmount());
    }

    @Test
    void pullDomainEvents_afterConfirm_returnsOneEventAndClearsInternalList() {
        Reservation reservation = reservation();
        reservation.confirm();

        List<DomainEvent> events = reservation.pullDomainEvents();

        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof ReservationConfirmedEvent);
        assertTrue(reservation.pullDomainEvents().isEmpty());
    }

    private static Reservation reservation() {
        return Reservation.create(CUSTOMER_ID, VEHICLE_ID, new DateRange(START, START.plusDays(3)),
                money("100.00"), money("20.00"), money("10.00"));
    }

    private static Reservation confirmedReservation() {
        Reservation reservation = reservation();
        reservation.confirm();
        return reservation;
    }

    private static Reservation activeReservation() {
        Reservation reservation = confirmedReservation();
        reservation.activate();
        return reservation;
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    private static void setStatus(Reservation reservation, ReservationStatus status) throws Exception {
        Field field = Reservation.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(reservation, status);
    }

    @SuppressWarnings("unchecked")
    private static void setExtras(Reservation reservation, List<ReservationExtra> extras) throws Exception {
        Field field = Reservation.class.getDeclaredField("extras");
        field.setAccessible(true);
        ((ArrayList<ReservationExtra>) field.get(reservation)).addAll(extras);
    }
}
