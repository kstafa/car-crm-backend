package com.rentflow.reservation.service;

import com.rentflow.customer.Customer;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.command.CancelReservationCommand;
import com.rentflow.reservation.command.ConfirmReservationCommand;
import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.reservation.model.ReservationDetail;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.BlacklistedCustomerException;
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
        when(vehicleAvailabilityPort.isAvailable(eq(vehicleId), any(DateRange.class))).thenReturn(true);
        CreateReservationCommand command = createCommand();

        ReservationId id = service.create(command);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(ReservationStatus.DRAFT, captor.getValue().getStatus());
        verify(auditLogPort).log(any());
        verify(eventPublisher, never()).publish(any());
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
    void create_vehicleUnavailable_throwsVehicleNotAvailableExceptionAndNeverSaves() {
        stubCustomer(Customer.create(customerId, "Ada", "Lovelace", "ada@example.com"));
        stubVehicle(vehicle());
        when(vehicleAvailabilityPort.isAvailable(eq(vehicleId), any(DateRange.class))).thenReturn(false);

        assertThrows(VehicleNotAvailableException.class, () -> service.create(createCommand()));
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void create_validParams_invalidatesCacheForVehicle() {
        stubCustomer(Customer.create(customerId, "Ada", "Lovelace", "ada@example.com"));
        stubVehicle(vehicle());
        when(vehicleAvailabilityPort.isAvailable(eq(vehicleId), any(DateRange.class))).thenReturn(true);

        service.create(createCommand());

        verify(availabilityCachePort).invalidate(vehicleId);
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

    private CreateReservationCommand createCommand() {
        return new CreateReservationCommand(customerId, vehicleId, PICKUP, PICKUP.plusDays(3), staffId);
    }

    private Reservation reservation() {
        return Reservation.create(customerId, vehicleId, new DateRange(PICKUP, PICKUP.plusDays(3)),
                money("300.00"), Money.zero(EUR), Money.zero(EUR));
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
