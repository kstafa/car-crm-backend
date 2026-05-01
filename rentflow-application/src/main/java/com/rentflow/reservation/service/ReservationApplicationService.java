package com.rentflow.reservation.service;

import com.rentflow.customer.Customer;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationPricingService;
import com.rentflow.reservation.command.CancelReservationCommand;
import com.rentflow.reservation.command.ConfirmReservationCommand;
import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.reservation.model.ReservationDetail;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.port.in.CancelReservationUseCase;
import com.rentflow.reservation.port.in.ConfirmReservationUseCase;
import com.rentflow.reservation.port.in.CreateReservationUseCase;
import com.rentflow.reservation.port.in.GetReservationUseCase;
import com.rentflow.reservation.port.in.ListReservationsUseCase;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.reservation.query.ListReservationsQuery;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.BlacklistedCustomerException;
import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;

@Service
@Transactional
public class ReservationApplicationService implements CreateReservationUseCase, ConfirmReservationUseCase,
        CancelReservationUseCase, GetReservationUseCase, ListReservationsUseCase {

    private static final Currency EUR = Currency.getInstance("EUR");

    private final ReservationRepository reservationRepository;
    private final VehicleAvailabilityPort vehicleAvailabilityPort;
    private final AvailabilityCachePort availabilityCachePort;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogPort auditLogPort;
    private final ReservationPricingService pricingService = new ReservationPricingService();

    public ReservationApplicationService(ReservationRepository reservationRepository,
                                         VehicleAvailabilityPort vehicleAvailabilityPort,
                                         AvailabilityCachePort availabilityCachePort,
                                         VehicleRepository vehicleRepository,
                                         CustomerRepository customerRepository,
                                         DomainEventPublisher eventPublisher,
                                         AuditLogPort auditLogPort) {
        this.reservationRepository = reservationRepository;
        this.vehicleAvailabilityPort = vehicleAvailabilityPort;
        this.availabilityCachePort = availabilityCachePort;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public ReservationId create(CreateReservationCommand cmd) {
        Customer customer = customerRepository.findById(cmd.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cmd.customerId().value()));
        vehicleRepository.findById(cmd.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + cmd.vehicleId().value()));
        if (customer.isBlacklisted()) {
            throw new BlacklistedCustomerException(cmd.customerId());
        }

        DateRange period = new DateRange(cmd.pickupDatetime(), cmd.returnDatetime());
        if (!vehicleAvailabilityPort.isAvailable(cmd.vehicleId(), period)) {
            throw new VehicleNotAvailableException(cmd.vehicleId(), period);
        }

        Money dailyRate = new Money(new BigDecimal("100.00"), EUR);
        Money baseAmount = pricingService.calculateBaseAmount(dailyRate, period);
        Reservation reservation = Reservation.create(cmd.customerId(), cmd.vehicleId(), period, baseAmount,
                Money.zero(EUR), Money.zero(EUR));

        reservationRepository.save(reservation);
        publishEvents(reservation);
        auditLogPort.log(AuditEntry.of("CREATE_RESERVATION", reservation.getId(), cmd.createdBy()));
        availabilityCachePort.invalidate(reservation.getVehicleId());
        return reservation.getId();
    }

    @Override
    public void confirm(ConfirmReservationCommand cmd) {
        Reservation reservation = loadReservation(cmd.reservationId());
        if (!vehicleAvailabilityPort.isAvailable(reservation.getVehicleId(), reservation.getRentalPeriod())) {
            throw new VehicleNotAvailableException(reservation.getVehicleId(), reservation.getRentalPeriod());
        }

        reservation.confirm();
        reservationRepository.save(reservation);
        publishEvents(reservation);
        auditLogPort.log(AuditEntry.of("CONFIRM_RESERVATION", reservation.getId(), cmd.confirmedBy()));
        availabilityCachePort.invalidate(reservation.getVehicleId());
    }

    @Override
    public void cancel(CancelReservationCommand cmd) {
        Reservation reservation = loadReservation(cmd.reservationId());
        reservation.cancel(cmd.reason());
        reservationRepository.save(reservation);
        publishEvents(reservation);
        auditLogPort.log(AuditEntry.of("CANCEL_RESERVATION", reservation.getId(), cmd.cancelledBy()));
        availabilityCachePort.invalidate(reservation.getVehicleId());
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationDetail get(ReservationId id) {
        Reservation reservation = loadReservation(id);
        return new ReservationDetail(
                reservation.getId(),
                reservation.getReservationNumber(),
                reservation.getCustomerId(),
                reservation.getVehicleId(),
                reservation.getRentalPeriod(),
                reservation.getStatus(),
                reservation.getBaseAmount(),
                reservation.getDiscountAmount(),
                reservation.getDepositAmount(),
                reservation.getTaxAmount(),
                reservation.totalAmount(),
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationSummary> list(ListReservationsQuery q) {
        return reservationRepository.findAll(q);
    }

    private Reservation loadReservation(ReservationId id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id.value()));
    }

    private void publishEvents(Reservation reservation) {
        for (DomainEvent event : reservation.pullDomainEvents()) {
            eventPublisher.publish(event);
        }
    }
}
