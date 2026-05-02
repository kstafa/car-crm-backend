package com.rentflow.contract.service;

import com.rentflow.contract.Contract;
import com.rentflow.contract.ContractOpenedEvent;
import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageReport;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.DamageReportStatus;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.contract.Inspection;
import com.rentflow.contract.InspectionChecklist;
import com.rentflow.contract.command.CreateDamageReportCommand;
import com.rentflow.contract.command.ExtendContractCommand;
import com.rentflow.contract.command.OpenContractCommand;
import com.rentflow.contract.command.RecordPickupCommand;
import com.rentflow.contract.command.RecordReturnCommand;
import com.rentflow.contract.command.UploadDamagePhotoCommand;
import com.rentflow.contract.command.UploadPhotoCommand;
import com.rentflow.contract.model.ContractDetail;
import com.rentflow.contract.model.ReturnSummary;
import com.rentflow.contract.port.out.ContractRepository;
import com.rentflow.contract.port.out.DamageReportRepository;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.VehicleStatus;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationPricingService;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import com.rentflow.shared.port.out.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
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
class ContractApplicationServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final ZonedDateTime RETURN = PICKUP.plusDays(3);

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private DamageReportRepository damageReportRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleAvailabilityPort availabilityPort;
    @Mock
    private AvailabilityCachePort cachePort;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private AuditLogPort auditLog;
    @Mock
    private FileStoragePort fileStorage;
    @Mock
    private ReservationPricingService pricingService;

    private ContractApplicationService service;
    private StaffId staffId;

    @BeforeEach
    void setUp() {
        service = new ContractApplicationService(contractRepository, damageReportRepository, reservationRepository,
                vehicleRepository, availabilityPort, cachePort, eventPublisher, auditLog, fileStorage, pricingService);
        staffId = StaffId.generate();
        lenient().when(pricingService.calculateLateFee(any(), any(), any())).thenReturn(money("0.00"));
        lenient().when(pricingService.calculateFuelSurcharge(any(), any(), any(), anyInt())).thenReturn(money("0.00"));
    }

    @Test
    void open_confirmedReservation_savesContractAndActivatesReservationAndRentsVehicle() {
        Reservation reservation = reservation(ReservationStatus.CONFIRMED);
        Vehicle vehicle = vehicle(reservation.getVehicleId(), VehicleStatus.AVAILABLE);
        stubOpen(reservation, vehicle);

        ContractId id = service.open(new OpenContractCommand(reservation.getId(), staffId));

        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());
        assertEquals(id, contractCaptor.getValue().getId());
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
        assertEquals(VehicleStatus.RENTED, vehicle.getStatus());
        verify(reservationRepository).save(reservation);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void open_reservationNotFound_throwsResourceNotFoundException() {
        ReservationId reservationId = ReservationId.generate();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.open(new OpenContractCommand(reservationId, staffId)));
    }

    @Test
    void open_reservationNotConfirmed_throwsInvalidStateTransition() {
        Reservation reservation = reservation(ReservationStatus.DRAFT);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.open(new OpenContractCommand(reservation.getId(), staffId)));
    }

    @Test
    void open_contractAlreadyExists_throwsDomainException() {
        Reservation reservation = reservation(ReservationStatus.CONFIRMED);
        Contract existing = contract(reservation);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(contractRepository.findByReservationId(reservation.getId())).thenReturn(Optional.of(existing));

        assertThrows(DomainException.class, () -> service.open(new OpenContractCommand(reservation.getId(), staffId)));
    }

    @Test
    void open_publishesContractOpenedEvent() {
        Reservation reservation = reservation(ReservationStatus.CONFIRMED);
        Vehicle vehicle = vehicle(reservation.getVehicleId(), VehicleStatus.AVAILABLE);
        stubOpen(reservation, vehicle);

        service.open(new OpenContractCommand(reservation.getId(), staffId));

        verify(eventPublisher).publish(any(ContractOpenedEvent.class));
    }

    @Test
    void recordPickup_validCommand_savesContractWithPreInspection() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        Vehicle vehicle = vehicle(reservation.getVehicleId(), VehicleStatus.RENTED);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        service.recordPickup(pickupCommand(contract.getId()));

        assertNotNull(contract.getPreInspection());
        verify(contractRepository).save(contract);
    }

    @Test
    void recordPickup_contractNotFound_throwsResourceNotFoundException() {
        ContractId contractId = ContractId.generate();
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.recordPickup(pickupCommand(contractId)));
    }

    @Test
    void recordPickup_alreadyPickedUp_throwsInvalidStateTransition() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = pickedUpContract(reservation);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.recordPickup(pickupCommand(contract.getId())));
    }

    @Test
    void recordPickup_updatesVehicleMileage() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        Vehicle vehicle = vehicle(reservation.getVehicleId(), VehicleStatus.RENTED);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        service.recordPickup(pickupCommand(contract.getId()));

        assertEquals(1500, vehicle.getCurrentMileage());
    }

    @Test
    void recordReturn_noDamage_completesContractAndMakesVehicleAvailable() {
        Flow flow = returnFlow(InspectionChecklist.allOk(), null);

        ReturnSummary summary = service.recordReturn(returnCommand(flow.contract().getId(), InspectionChecklist.allOk(),
                null));

        assertFalse(summary.damageDetected());
        assertEquals(VehicleStatus.AVAILABLE, flow.vehicle().getStatus());
        assertEquals(ReservationStatus.COMPLETED, flow.reservation().getStatus());
        assertEquals(com.rentflow.contract.ContractStatus.COMPLETED, flow.contract().getStatus());
    }

    @Test
    void recordReturn_minorDamage_completesContractAndMakesVehicleAvailableAndCreatesDamageReport() {
        Flow flow = returnFlow(damagedChecklist(), DamageSeverity.MINOR);

        ReturnSummary summary = service.recordReturn(returnCommand(flow.contract().getId(), damagedChecklist(),
                DamageSeverity.MINOR));

        assertTrue(summary.damageDetected());
        assertNotNull(summary.damageReportId());
        assertEquals(VehicleStatus.AVAILABLE, flow.vehicle().getStatus());
        verify(damageReportRepository).save(any(DamageReport.class));
    }

    @Test
    void recordReturn_majorDamage_sendsVehicleToMaintenanceInsteadOfAvailable() {
        Flow flow = returnFlow(damagedChecklist(), DamageSeverity.MAJOR);

        service.recordReturn(returnCommand(flow.contract().getId(), damagedChecklist(), DamageSeverity.MAJOR));

        assertEquals(VehicleStatus.MAINTENANCE, flow.vehicle().getStatus());
    }

    @Test
    void recordReturn_lateReturn_calculatesLateFee() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE, ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().minusHours(3));
        Flow flow = returnFlow(reservation, InspectionChecklist.allOk(), null);
        when(pricingService.calculateLateFee(eq(flow.contract().getScheduledReturn()), any(ZonedDateTime.class),
                eq(ContractApplicationService.LATE_FEE_HOURLY_RATE))).thenReturn(money("45.00"));

        ReturnSummary summary = service.recordReturn(returnCommand(flow.contract().getId(), InspectionChecklist.allOk(),
                null));

        assertEquals(money("45.00"), summary.lateFee());
        verify(pricingService).calculateLateFee(eq(flow.contract().getScheduledReturn()), any(ZonedDateTime.class),
                eq(ContractApplicationService.LATE_FEE_HOURLY_RATE));
    }

    @Test
    void recordReturn_fuelDeficit_calculatesFuelSurcharge() {
        Flow flow = returnFlow(InspectionChecklist.allOk(), null);
        when(pricingService.calculateFuelSurcharge(eq(FuelLevel.FULL), eq(FuelLevel.HALF),
                eq(ContractApplicationService.FUEL_RATE_PER_LITER),
                eq(ContractApplicationService.TANK_CAPACITY_LITERS))).thenReturn(money("62.50"));

        ReturnSummary summary = service.recordReturn(returnCommand(flow.contract().getId(), InspectionChecklist.allOk(),
                null, FuelLevel.HALF));

        assertEquals(money("62.50"), summary.fuelSurcharge());
    }

    @Test
    void recordReturn_onTimeReturnFullFuel_zeroSurcharges() {
        Flow flow = returnFlow(InspectionChecklist.allOk(), null);

        ReturnSummary summary = service.recordReturn(returnCommand(flow.contract().getId(), InspectionChecklist.allOk(),
                null));

        assertEquals(money("0.00"), summary.lateFee());
        assertEquals(money("0.00"), summary.fuelSurcharge());
        assertEquals(money("0.00"), summary.totalSurcharges());
    }

    @Test
    void recordReturn_contractNotFound_throwsResourceNotFoundException() {
        ContractId contractId = ContractId.generate();
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.recordReturn(returnCommand(contractId, InspectionChecklist.allOk(), null)));
    }

    @Test
    void recordReturn_beforePickupRecorded_throwsInvalidStateTransition() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        Vehicle vehicle = vehicle(reservation.getVehicleId(), VehicleStatus.RENTED);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.recordReturn(returnCommand(contract.getId(), InspectionChecklist.allOk(), null)));
    }

    @Test
    void recordReturn_invalidatesAvailabilityCache() {
        Flow flow = returnFlow(InspectionChecklist.allOk(), null);

        service.recordReturn(returnCommand(flow.contract().getId(), InspectionChecklist.allOk(), null));

        verify(cachePort).invalidate(flow.vehicle().getId());
    }

    @Test
    void extend_vehicleFreeForExtension_updatesContractAndReservation() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(availabilityPort.isAvailable(eq(contract.getVehicleId()), any(DateRange.class))).thenReturn(true);

        service.extend(new ExtendContractCommand(contract.getId(), RETURN.plusDays(2), staffId));

        assertEquals(RETURN.plusDays(2), contract.getScheduledReturn());
        assertEquals(RETURN.plusDays(2), reservation.getRentalPeriod().end());
        verify(contractRepository).save(contract);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void extend_vehicleBooked_throwsVehicleNotAvailableException() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(availabilityPort.isAvailable(eq(contract.getVehicleId()), any(DateRange.class))).thenReturn(false);

        assertThrows(VehicleNotAvailableException.class,
                () -> service.extend(new ExtendContractCommand(contract.getId(), RETURN.plusDays(2), staffId)));
    }

    @Test
    void extend_contractNotActive_throwsInvalidStateTransition() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = pickedUpContract(reservation);
        contract.recordReturn(postInspection(InspectionChecklist.allOk()), RETURN);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(availabilityPort.isAvailable(eq(contract.getVehicleId()), any(DateRange.class))).thenReturn(true);

        assertThrows(InvalidStateTransitionException.class,
                () -> service.extend(new ExtendContractCommand(contract.getId(), RETURN.plusDays(2), staffId)));
    }

    @Test
    void uploadPhoto_validFile_uploadsToStorageAndReturnsKey() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(fileStorage.upload(any(), eq("contracts/" + contract.getId().value()), any(), eq("image/jpeg")))
                .thenReturn("contracts/" + contract.getId().value() + "/a.jpg");

        String key = service.upload(new UploadPhotoCommand(contract.getId(), new byte[]{1}, "a.jpg", "image/jpeg",
                staffId));

        assertEquals("contracts/" + contract.getId().value() + "/a.jpg", key);
    }

    @Test
    void uploadPhoto_contractNotFound_throwsResourceNotFoundException() {
        ContractId contractId = ContractId.generate();
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.upload(new UploadPhotoCommand(contractId,
                new byte[]{1}, "a.jpg", "image/jpeg", staffId)));
    }

    @Test
    void createDamageReport_validCommand_savesAndReturnsId() {
        CreateDamageReportCommand command = new CreateDamageReportCommand(VehicleId.generate(), ContractId.generate(),
                CustomerId.generate(), "Scratch", DamageSeverity.MINOR, DamageLiability.CUSTOMER, money("100.00"),
                staffId);

        DamageReportId id = service.create(command);

        ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
        verify(damageReportRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
    }

    @Test
    void uploadDamagePhoto_validFile_addsKeyToReport() {
        DamageReport report = DamageReport.report(VehicleId.generate(), ContractId.generate(), CustomerId.generate(),
                "Scratch", DamageSeverity.MINOR, DamageLiability.CUSTOMER, money("100.00"));
        when(damageReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(fileStorage.upload(any(), eq("damage-reports/" + report.getId().value()), any(), eq("image/jpeg")))
                .thenReturn("damage-reports/" + report.getId().value() + "/a.jpg");

        String key = service.upload(new UploadDamagePhotoCommand(report.getId(), new byte[]{1}, "a.jpg",
                "image/jpeg", staffId));

        assertEquals("damage-reports/" + report.getId().value() + "/a.jpg", key);
        assertEquals(List.of(key), report.getPhotoKeys());
        verify(damageReportRepository).save(report);
    }

    @Test
    void get_existingContract_returnsDetail() {
        Reservation reservation = reservation(ReservationStatus.ACTIVE);
        Contract contract = contract(reservation);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        ContractDetail detail = service.get(contract.getId());

        assertEquals(contract.getId(), detail.id());
        assertEquals(contract.getContractNumber(), detail.contractNumber());
    }

    private void stubOpen(Reservation reservation, Vehicle vehicle) {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(contractRepository.findByReservationId(reservation.getId())).thenReturn(Optional.empty());
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
    }

    private Flow returnFlow(InspectionChecklist checklist, DamageSeverity severity) {
        return returnFlow(reservation(ReservationStatus.ACTIVE), checklist, severity);
    }

    private Flow returnFlow(Reservation reservation, InspectionChecklist checklist, DamageSeverity severity) {
        Contract contract = pickedUpContract(reservation);
        Vehicle vehicle = vehicle(reservation.getVehicleId(), VehicleStatus.RENTED);
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        return new Flow(reservation, contract, vehicle, checklist, severity);
    }

    private RecordPickupCommand pickupCommand(ContractId contractId) {
        return new RecordPickupCommand(contractId, InspectionChecklist.allOk(), FuelLevel.FULL, 1500, List.of(),
                staffId);
    }

    private RecordReturnCommand returnCommand(ContractId contractId, InspectionChecklist checklist,
                                              DamageSeverity severity) {
        return returnCommand(contractId, checklist, severity, FuelLevel.FULL);
    }

    private RecordReturnCommand returnCommand(ContractId contractId, InspectionChecklist checklist,
                                              DamageSeverity severity, FuelLevel fuelLevel) {
        return new RecordReturnCommand(contractId, checklist, fuelLevel, 1800, List.of(),
                severity == null ? null : "Front bumper dented", severity,
                severity == null ? null : DamageLiability.CUSTOMER,
                severity == null ? null : money("250.00"), staffId);
    }

    private static Contract pickedUpContract(Reservation reservation) {
        Contract contract = contract(reservation);
        contract.recordPickup(preInspection(), PICKUP);
        contract.pullDomainEvents();
        return contract;
    }

    private static Contract contract(Reservation reservation) {
        Contract contract = Contract.open(reservation.getId(), reservation.getCustomerId(), reservation.getVehicleId(),
                reservation.getRentalPeriod().start(), reservation.getRentalPeriod().end());
        contract.pullDomainEvents();
        return contract;
    }

    private static Reservation reservation(ReservationStatus status) {
        return reservation(status, PICKUP, RETURN);
    }

    private static Reservation reservation(ReservationStatus status, ZonedDateTime pickup, ZonedDateTime returned) {
        Reservation reservation = Reservation.create(CustomerId.generate(), VehicleId.generate(),
                new DateRange(pickup, returned), money("300.00"), money("0.00"), money("0.00"));
        if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.ACTIVE
                || status == ReservationStatus.COMPLETED) {
            reservation.confirm();
        }
        if (status == ReservationStatus.ACTIVE || status == ReservationStatus.COMPLETED) {
            reservation.activate();
        }
        if (status == ReservationStatus.COMPLETED) {
            reservation.complete();
        }
        reservation.pullDomainEvents();
        return reservation;
    }

    private static Vehicle vehicle(VehicleId vehicleId, VehicleStatus status) {
        Vehicle vehicle = Vehicle.reconstitute(vehicleId, "AA-123-AA", "Toyota", "Yaris", 2024,
                VehicleCategoryId.generate(), 1000, VehicleStatus.AVAILABLE, true, "Compact", List.of());
        if (status == VehicleStatus.RENTED) {
            vehicle.markAsRented();
        } else if (status == VehicleStatus.MAINTENANCE) {
            vehicle.sendToMaintenance();
        }
        vehicle.pullDomainEvents();
        return vehicle;
    }

    private static Inspection preInspection() {
        return new Inspection(Inspection.InspectionType.PRE, InspectionChecklist.allOk(), FuelLevel.FULL, 1000,
                List.of(), Instant.now(), StaffId.generate());
    }

    private static Inspection postInspection(InspectionChecklist checklist) {
        return new Inspection(Inspection.InspectionType.POST, checklist, FuelLevel.FULL, 1800, List.of(),
                Instant.now(), StaffId.generate());
    }

    private static InspectionChecklist damagedChecklist() {
        return new InspectionChecklist(false, true, true, true, true, true, true, true, "front dent");
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    private record Flow(Reservation reservation, Contract contract, Vehicle vehicle, InspectionChecklist checklist,
                        DamageSeverity severity) {
    }
}
