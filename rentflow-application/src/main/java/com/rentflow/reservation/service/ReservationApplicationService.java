package com.rentflow.reservation.service;

import com.rentflow.customer.Customer;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.VehicleCategory;
import com.rentflow.fleet.port.out.VehicleCategoryRepository;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationPricingService;
import com.rentflow.reservation.command.ApplyDiscountCommand;
import com.rentflow.reservation.command.CancelReservationCommand;
import com.rentflow.reservation.command.ConfirmReservationCommand;
import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.reservation.command.ExtendReservationCommand;
import com.rentflow.reservation.model.CalendarEntry;
import com.rentflow.reservation.model.ConflictSummary;
import com.rentflow.reservation.model.ReservationDetail;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.port.in.ApplyDiscountUseCase;
import com.rentflow.reservation.port.in.CancelReservationUseCase;
import com.rentflow.reservation.port.in.ConfirmReservationUseCase;
import com.rentflow.reservation.port.in.CreateReservationUseCase;
import com.rentflow.reservation.port.in.ExtendReservationUseCase;
import com.rentflow.reservation.port.in.GetReservationCalendarUseCase;
import com.rentflow.reservation.port.in.GetReservationConflictsUseCase;
import com.rentflow.reservation.port.in.GetReservationUseCase;
import com.rentflow.reservation.port.in.ListOverdueUseCase;
import com.rentflow.reservation.port.in.ListReservationsUseCase;
import com.rentflow.reservation.port.in.ListTodayPickupsUseCase;
import com.rentflow.reservation.port.in.ListTodayReturnsUseCase;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.reservation.query.GetCalendarQuery;
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

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ReservationApplicationService implements CreateReservationUseCase, ConfirmReservationUseCase,
        CancelReservationUseCase, ExtendReservationUseCase, ApplyDiscountUseCase, GetReservationUseCase,
        ListReservationsUseCase, GetReservationCalendarUseCase, GetReservationConflictsUseCase,
        ListTodayPickupsUseCase, ListTodayReturnsUseCase, ListOverdueUseCase {

    private final ReservationRepository reservationRepository;
    private final VehicleAvailabilityPort vehicleAvailabilityPort;
    private final AvailabilityCachePort availabilityCachePort;
    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogPort auditLogPort;
    private final ReservationPricingService pricingService = new ReservationPricingService();

    public ReservationApplicationService(ReservationRepository reservationRepository,
                                         VehicleAvailabilityPort vehicleAvailabilityPort,
                                         AvailabilityCachePort availabilityCachePort,
                                         VehicleRepository vehicleRepository,
                                         VehicleCategoryRepository categoryRepository,
                                         CustomerRepository customerRepository,
                                         DomainEventPublisher eventPublisher,
                                         AuditLogPort auditLogPort) {
        this.reservationRepository = reservationRepository;
        this.vehicleAvailabilityPort = vehicleAvailabilityPort;
        this.availabilityCachePort = availabilityCachePort;
        this.vehicleRepository = vehicleRepository;
        this.categoryRepository = categoryRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public ReservationId create(CreateReservationCommand cmd) {
        Customer customer = customerRepository.findById(cmd.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cmd.customerId().value()));
        Vehicle vehicle = vehicleRepository.findById(cmd.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + cmd.vehicleId().value()));
        if (customer.isBlacklisted()) {
            throw new BlacklistedCustomerException(cmd.customerId());
        }
        VehicleCategory category = categoryRepository.findById(vehicle.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle category not found: "
                        + vehicle.getCategoryId().value()));

        DateRange period = new DateRange(cmd.pickupDatetime(), cmd.returnDatetime());
        Money dailyRate = category.getBaseDailyRate();
        Money baseAmount = pricingService.calculateBaseAmount(dailyRate, period);
        Money taxAmount = baseAmount.multiply(category.getTaxRate());
        Reservation reservation = Reservation.create(cmd.customerId(), cmd.vehicleId(), period, baseAmount,
                category.getDepositAmount(), taxAmount);

        reservationRepository.save(reservation);
        publishEvents(reservation);
        auditLogPort.log(AuditEntry.of("CREATE_RESERVATION", reservation.getId(), cmd.createdBy()));
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
    public void extend(ExtendReservationCommand cmd) {
        Reservation reservation = loadReservation(cmd.reservationId());
        DateRange extensionPeriod = new DateRange(reservation.getRentalPeriod().end(), cmd.newReturnDatetime());
        if (!vehicleAvailabilityPort.isAvailable(reservation.getVehicleId(), extensionPeriod)) {
            throw new VehicleNotAvailableException(reservation.getVehicleId(), extensionPeriod);
        }

        reservation.extend(cmd.newReturnDatetime());
        reservationRepository.save(reservation);
        publishEvents(reservation);
        auditLogPort.log(AuditEntry.of("RESERVATION_EXTENDED", reservation.getId(), cmd.extendedBy()));
        availabilityCachePort.invalidate(reservation.getVehicleId());
    }

    @Override
    public void apply(ApplyDiscountCommand cmd) {
        Reservation reservation = loadReservation(cmd.reservationId());
        Money discountAmount = reservation.getBaseAmount().multiply(cmd.discountPercent());
        reservation.applyDiscount(discountAmount);
        reservationRepository.save(reservation);
        auditLogPort.log(AuditEntry.of("DISCOUNT_APPLIED", reservation.getId(), cmd.appliedBy()));
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

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEntry> getCalendar(GetCalendarQuery query) {
        return reservationRepository.findForCalendar(query.from(), query.to(), query.categoryId())
                .stream()
                .map(row -> new CalendarEntry(
                        row.reservationId(),
                        row.reservationNumber(),
                        row.vehicleId(),
                        row.vehicleLicensePlate(),
                        row.vehicleBrand(),
                        row.vehicleModel(),
                        row.customerId(),
                        row.customerFirstName() + " " + row.customerLastName(),
                        row.pickupDatetime(),
                        row.returnDatetime(),
                        row.status()))
                .sorted(Comparator.comparing(CalendarEntry::pickupDatetime))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictSummary> getConflicts() {
        return reservationRepository.findDraftConflicts()
                .stream()
                .map(row -> new ConflictSummary(
                        row.draftId(),
                        row.draftNumber(),
                        row.vehicleId(),
                        new DateRange(row.draftStart(), row.draftEnd()),
                        row.conflictingId(),
                        row.conflictingNumber(),
                        row.conflictingStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationSummary> listPickups() {
        return reservationRepository.findTodayPickups();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationSummary> listReturns() {
        return reservationRepository.findTodayReturns();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationSummary> listOverdue() {
        return reservationRepository.findOverdue();
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
