package com.rentflow.reservation.service;

import com.rentflow.customer.Customer;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.command.ApplyDiscountCommand;
import com.rentflow.reservation.command.CancelReservationCommand;
import com.rentflow.reservation.command.ConfirmReservationCommand;
import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.reservation.command.ExtendReservationCommand;
import com.rentflow.reservation.model.CalendarEntry;
import com.rentflow.reservation.model.ConflictRow;
import com.rentflow.reservation.model.ConflictSummary;
import com.rentflow.reservation.model.ReservationCalendarRow;
import com.rentflow.reservation.model.ReservationDetail;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.query.GetCalendarQuery;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.BlacklistedCustomerException;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationApplicationServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private VehicleAvailabilityPort vehicleAvailabilityPort;
    @Mock
    private AvailabilityCachePort availabilityCachePort;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private AuditLogPort auditLogPort;

    private ReservationApplicationService service;
    private CustomerId customerId;
    private VehicleId vehicleId;
    private StaffId staffId;

    @BeforeEach
    void setUp() {
        service = new ReservationApplicationService(
                reservationRepository,
                vehicleAvailabilityPort,
                availabilityCachePort,
                vehicleRepository,
                customerRepository,
                eventPublisher,
                auditLogPort
        );
        customerId = CustomerId.generate();
        vehicleId = VehicleId.generate();
        staffId = StaffId.generate();
    }

    @Test
    void create_allValid_savesAndPublishesEventAndAudits() {
        stubCustomer(Customer.create(customerId, "Ada", "Lovelace", "ada@example.com"));
        stubVehicle(vehicle());
        CreateReservationCommand command = createCommand();

        ReservationId id = service.create(command);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(ReservationStatus.DRAFT, captor.getValue().getStatus());
        verify(auditLogPort).log(any());
        verify(eventPublisher, never()).publish(any());
        verify(vehicleAvailabilityPort, never()).isAvailable(any(), any());
    }

    @Test
    void create_customerNotFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(createCommand()));
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void create_vehicleNotFound_throwsResourceNotFoundException() {
        stubCustomer(Customer.create(customerId, "Ada", "Lovelace", "ada@example.com"));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(createCommand()));
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void create_blacklistedCustomer_throwsBlacklistedCustomerExceptionAndNeverSaves() {
        Customer customer = Customer.create(customerId, "Ada", "Lovelace", "ada@example.com");
        customer.blacklist("fraud");
        stubCustomer(customer);
        stubVehicle(vehicle());

        assertThrows(BlacklistedCustomerException.class, () -> service.create(createCommand()));
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void create_vehicleUnavailable_savesDraftWithoutCheckingAvailability() {
        stubCustomer(Customer.create(customerId, "Ada", "Lovelace", "ada@example.com"));
        stubVehicle(vehicle());

        service.create(createCommand());

        verify(reservationRepository).save(any());
        verify(vehicleAvailabilityPort, never()).isAvailable(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void create_validParams_doesNotInvalidateAvailabilityCache() {
        stubCustomer(Customer.create(customerId, "Ada", "Lovelace", "ada@example.com"));
        stubVehicle(vehicle());

        service.create(createCommand());

        verify(availabilityCachePort, never()).invalidate(any());
    }

    @Test
    void confirm_vehicleStillAvailable_confirmsAndPublishesEventAndSaves() {
        Reservation reservation = reservation();
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(vehicleAvailabilityPort.isAvailable(reservation.getVehicleId(), reservation.getRentalPeriod())).thenReturn(true);

        service.confirm(new ConfirmReservationCommand(reservation.getId(), staffId));

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
        verify(eventPublisher).publish(any());
        verify(auditLogPort).log(any());
        verify(availabilityCachePort).invalidate(reservation.getVehicleId());
    }

    @Test
    void confirm_vehicleNoLongerAvailable_throwsVehicleNotAvailableException() {
        Reservation reservation = reservation();
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(vehicleAvailabilityPort.isAvailable(reservation.getVehicleId(), reservation.getRentalPeriod())).thenReturn(false);

        assertThrows(VehicleNotAvailableException.class,
                () -> service.confirm(new ConfirmReservationCommand(reservation.getId(), staffId)));
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void confirm_reservationNotFound_throwsResourceNotFoundException() {
        ReservationId reservationId = ReservationId.generate();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.confirm(new ConfirmReservationCommand(reservationId, staffId)));
    }

    @Test
    void cancel_fromConfirmed_cancelsAndPublishesEvent() {
        Reservation reservation = reservation();
        reservation.confirm();
        reservation.pullDomainEvents();
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        service.cancel(new CancelReservationCommand(reservation.getId(), "customer request", staffId));

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
        verify(eventPublisher).publish(any());
    }

    @Test
    void cancel_reservationNotFound_throwsResourceNotFoundException() {
        ReservationId reservationId = ReservationId.generate();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.cancel(new CancelReservationCommand(reservationId, "customer request", staffId)));
    }

    @Test
    void get_exists_returnsMappedDetail() {
        Reservation reservation = reservation();
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        ReservationDetail detail = service.get(reservation.getId());

        assertEquals(reservation.getId(), detail.id());
        assertEquals(reservation.getReservationNumber(), detail.reservationNumber());
        assertEquals(reservation.getCustomerId(), detail.customerId());
        assertEquals(reservation.getVehicleId(), detail.vehicleId());
        assertEquals(reservation.totalAmount(), detail.totalAmount());
    }

    @Test
    void get_notFound_throwsResourceNotFoundException() {
        ReservationId reservationId = ReservationId.generate();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(reservationId));
    }

    @Test
    void extend_vehicleFreeForExtensionPeriod_updatesReturnDateAndSaves() {
        Reservation reservation = activeReservation(PICKUP, PICKUP.plusDays(5));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(vehicleAvailabilityPort.isAvailable(eq(vehicleId), any(DateRange.class))).thenReturn(true);

        service.extend(new ExtendReservationCommand(reservation.getId(), PICKUP.plusDays(8), staffId));

        assertEquals(PICKUP.plusDays(8), reservation.getRentalPeriod().end());
        verify(reservationRepository).save(reservation);
        verify(eventPublisher).publish(any());
        verify(auditLogPort).log(any());
        verify(availabilityCachePort).invalidate(vehicleId);
    }

    @Test
    void extend_vehicleBookedForExtensionPeriod_throwsVehicleNotAvailableException() {
        Reservation reservation = activeReservation(PICKUP, PICKUP.plusDays(5));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(vehicleAvailabilityPort.isAvailable(eq(vehicleId), any(DateRange.class))).thenReturn(false);

        assertThrows(VehicleNotAvailableException.class,
                () -> service.extend(new ExtendReservationCommand(reservation.getId(), PICKUP.plusDays(8), staffId)));
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void extend_reservationNotFound_throwsResourceNotFoundException() {
        ReservationId reservationId = ReservationId.generate();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.extend(new ExtendReservationCommand(reservationId, PICKUP.plusDays(8), staffId)));
    }

    @Test
    void extend_callsAvailabilityCheckOnlyForExtensionPeriodNotFullPeriod() {
        Reservation reservation = activeReservation(PICKUP.plusDays(1), PICKUP.plusDays(5));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(vehicleAvailabilityPort.isAvailable(eq(vehicleId), any(DateRange.class))).thenReturn(true);

        service.extend(new ExtendReservationCommand(reservation.getId(), PICKUP.plusDays(8), staffId));

        ArgumentCaptor<DateRange> captor = ArgumentCaptor.forClass(DateRange.class);
        verify(vehicleAvailabilityPort).isAvailable(eq(vehicleId), captor.capture());
        assertEquals(PICKUP.plusDays(5), captor.getValue().start());
        assertEquals(PICKUP.plusDays(8), captor.getValue().end());
    }

    @Test
    void applyDiscount_validPercent_savesUpdatedReservation() {
        Reservation reservation = reservation();
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        service.apply(new ApplyDiscountCommand(reservation.getId(), new BigDecimal("0.15"), staffId));

        assertTrue(reservation.getDiscountAmount().equals(money("45.00")));
        verify(reservationRepository).save(reservation);
        verify(auditLogPort).log(any());
    }

    @Test
    void applyDiscount_discountExceedsBase_throwsDomainException() {
        Reservation reservation = reservation();

        assertThrows(IllegalArgumentException.class,
                () -> new ApplyDiscountCommand(reservation.getId(), new BigDecimal("1.01"), staffId));
        assertThrows(DomainException.class, () -> reservation.applyDiscount(money("301.00")));
    }

    @Test
    void applyDiscount_reservationNotFound_throwsResourceNotFoundException() {
        ReservationId reservationId = ReservationId.generate();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.apply(new ApplyDiscountCommand(reservationId, new BigDecimal("0.15"), staffId)));
    }

    @Test
    void applyDiscount_doesNotPublishEventOrInvalidateCache() {
        Reservation reservation = reservation();
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        service.apply(new ApplyDiscountCommand(reservation.getId(), new BigDecimal("0.10"), staffId));

        verify(eventPublisher, never()).publish(any());
        verify(availabilityCachePort, never()).invalidate(any());
    }

    @Test
    void getCalendar_delegatesQueryAndMapsRows() {
        ReservationId firstId = ReservationId.generate();
        ReservationId secondId = ReservationId.generate();
        GetCalendarQuery query = new GetCalendarQuery(PICKUP.toLocalDate(), PICKUP.plusDays(10).toLocalDate(),
                null);
        when(reservationRepository.findForCalendar(query.from(), query.to(), null))
                .thenReturn(List.of(
                        calendarRow(secondId, PICKUP.plusDays(2), "Grace", "Hopper"),
                        calendarRow(firstId, PICKUP.plusDays(1), "Ada", "Lovelace")));

        List<CalendarEntry> result = service.getCalendar(query);

        assertEquals(List.of(firstId, secondId), result.stream().map(CalendarEntry::reservationId).toList());
        assertEquals("Ada Lovelace", result.getFirst().customerName());
    }

    @Test
    void getConflicts_delegatesQueryAndMapsRows() {
        ReservationId draftId = ReservationId.generate();
        ReservationId conflictingId = ReservationId.generate();
        when(reservationRepository.findDraftConflicts())
                .thenReturn(List.of(new ConflictRow(draftId, "RES-DRAFT", vehicleId, PICKUP, PICKUP.plusDays(3),
                        conflictingId, "RES-CONFIRMED", ReservationStatus.CONFIRMED)));

        List<ConflictSummary> result = service.getConflicts();

        assertEquals(1, result.size());
        assertEquals(draftId, result.getFirst().draftReservationId());
        assertEquals(conflictingId, result.getFirst().conflictingReservationId());
        assertEquals(new DateRange(PICKUP, PICKUP.plusDays(3)), result.getFirst().period());
    }

    @Test
    void listPickups_delegatesToRepository() {
        ReservationSummary summary = reservationSummary();
        when(reservationRepository.findTodayPickups()).thenReturn(List.of(summary));

        assertEquals(List.of(summary), service.listPickups());
    }

    @Test
    void listReturns_delegatesToRepository() {
        ReservationSummary summary = reservationSummary();
        when(reservationRepository.findTodayReturns()).thenReturn(List.of(summary));

        assertEquals(List.of(summary), service.listReturns());
    }

    @Test
    void listOverdue_delegatesToRepository() {
        ReservationSummary summary = reservationSummary();
        when(reservationRepository.findOverdue()).thenReturn(List.of(summary));

        assertEquals(List.of(summary), service.listOverdue());
    }

    private CreateReservationCommand createCommand() {
        return new CreateReservationCommand(customerId, vehicleId, PICKUP, PICKUP.plusDays(3), staffId);
    }

    private Reservation reservation() {
        return Reservation.create(customerId, vehicleId, new DateRange(PICKUP, PICKUP.plusDays(3)),
                money("300.00"), Money.zero(EUR), Money.zero(EUR));
    }

    private Reservation activeReservation(ZonedDateTime pickup, ZonedDateTime returns) {
        Reservation reservation = Reservation.create(customerId, vehicleId, new DateRange(pickup, returns),
                money("300.00"), Money.zero(EUR), Money.zero(EUR));
        reservation.confirm();
        reservation.pullDomainEvents();
        reservation.activate();
        return reservation;
    }

    private ReservationCalendarRow calendarRow(ReservationId reservationId, ZonedDateTime pickup,
                                               String firstName, String lastName) {
        return new ReservationCalendarRow(
                reservationId,
                "RES-" + reservationId.value().toString().substring(0, 8),
                vehicleId,
                "AA-123-AA",
                "Toyota",
                "Yaris",
                customerId,
                firstName,
                lastName,
                pickup,
                pickup.plusDays(3),
                ReservationStatus.CONFIRMED);
    }

    private ReservationSummary reservationSummary() {
        return new ReservationSummary(ReservationId.generate(), "RES-12345678", customerId, vehicleId, PICKUP,
                PICKUP.plusDays(3), ReservationStatus.CONFIRMED, money("300.00"));
    }

    private Vehicle vehicle() {
        return Vehicle.register("AA-123-AA", "Toyota", "Yaris", 2024, VehicleCategoryId.generate(), 1000);
    }

    private void stubCustomer(Customer customer) {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    }

    private void stubVehicle(Vehicle vehicle) {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
