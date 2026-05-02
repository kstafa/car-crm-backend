package com.rentflow.contract.service;

import com.rentflow.contract.Contract;
import com.rentflow.contract.DamageReport;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.contract.Inspection;
import com.rentflow.contract.command.CreateDamageReportCommand;
import com.rentflow.contract.command.ExtendContractCommand;
import com.rentflow.contract.command.OpenContractCommand;
import com.rentflow.contract.command.RecordPickupCommand;
import com.rentflow.contract.command.RecordReturnCommand;
import com.rentflow.contract.command.UploadDamagePhotoCommand;
import com.rentflow.contract.command.UploadPhotoCommand;
import com.rentflow.contract.model.ContractDetail;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.model.DamageReportDetail;
import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.contract.model.ReturnSummary;
import com.rentflow.contract.port.in.CreateDamageReportUseCase;
import com.rentflow.contract.port.in.ExtendContractUseCase;
import com.rentflow.contract.port.in.GetContractUseCase;
import com.rentflow.contract.port.in.GetDamageReportUseCase;
import com.rentflow.contract.port.in.ListActiveContractsUseCase;
import com.rentflow.contract.port.in.ListContractsUseCase;
import com.rentflow.contract.port.in.ListDamageReportsUseCase;
import com.rentflow.contract.port.in.OpenContractUseCase;
import com.rentflow.contract.port.in.RecordPickupUseCase;
import com.rentflow.contract.port.in.RecordReturnUseCase;
import com.rentflow.contract.port.in.UploadContractPhotoUseCase;
import com.rentflow.contract.port.in.UploadDamagePhotoUseCase;
import com.rentflow.contract.port.out.ContractRepository;
import com.rentflow.contract.port.out.DamageReportRepository;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationPricingService;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.contract.query.ListContractsQuery;
import com.rentflow.contract.query.ListDamageReportsQuery;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import com.rentflow.shared.port.out.FileStoragePort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContractApplicationService implements OpenContractUseCase, RecordPickupUseCase, RecordReturnUseCase,
        ExtendContractUseCase, GetContractUseCase, ListContractsUseCase, ListActiveContractsUseCase,
        UploadContractPhotoUseCase, CreateDamageReportUseCase, UploadDamagePhotoUseCase, GetDamageReportUseCase,
        ListDamageReportsUseCase {

    static final Money LATE_FEE_HOURLY_RATE =
            new Money(new BigDecimal("15.00"), Currency.getInstance("EUR"));
    static final Money FUEL_RATE_PER_LITER =
            new Money(new BigDecimal("2.50"), Currency.getInstance("EUR"));
    static final int TANK_CAPACITY_LITERS = 50;

    private final ContractRepository contractRepository;
    private final DamageReportRepository damageReportRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityPort availabilityPort;
    private final AvailabilityCachePort cachePort;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogPort auditLogPort;
    private final FileStoragePort fileStoragePort;
    private final ReservationPricingService pricingService;

    public ContractApplicationService(ContractRepository contractRepository,
                                      DamageReportRepository damageReportRepository,
                                      ReservationRepository reservationRepository,
                                      VehicleRepository vehicleRepository,
                                      VehicleAvailabilityPort availabilityPort,
                                      AvailabilityCachePort cachePort,
                                      DomainEventPublisher eventPublisher,
                                      AuditLogPort auditLogPort,
                                      FileStoragePort fileStoragePort,
                                      ReservationPricingService pricingService) {
        this.contractRepository = contractRepository;
        this.damageReportRepository = damageReportRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityPort = availabilityPort;
        this.cachePort = cachePort;
        this.eventPublisher = eventPublisher;
        this.auditLogPort = auditLogPort;
        this.fileStoragePort = fileStoragePort;
        this.pricingService = pricingService;
    }

    @Override
    public ContractId open(OpenContractCommand command) {
        Reservation reservation = loadReservation(command.reservationId());
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidStateTransitionException("Reservation must be CONFIRMED before opening a contract");
        }
        contractRepository.findByReservationId(command.reservationId()).ifPresent(existing -> {
            throw new DomainException("Contract already exists for this reservation");
        });
        Vehicle vehicle = loadVehicle(reservation.getVehicleId());

        Contract contract = Contract.open(reservation.getId(), reservation.getCustomerId(), reservation.getVehicleId(),
                reservation.getRentalPeriod().start(), reservation.getRentalPeriod().end());
        reservation.activate();
        vehicle.markAsRented();

        contractRepository.save(contract);
        reservationRepository.save(reservation);
        vehicleRepository.save(vehicle);
        publishEvents(contract.pullDomainEvents());
        publishEvents(reservation.pullDomainEvents());
        publishEvents(vehicle.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("CONTRACT_OPENED", contract.getId(), command.openedBy()));
        return contract.getId();
    }

    @Override
    public void recordPickup(RecordPickupCommand command) {
        Contract contract = loadContract(command.contractId());
        Inspection preInspection = new Inspection(Inspection.InspectionType.PRE, command.preInspection(),
                command.startFuelLevel(), command.startMileage(), command.photoKeys(), Instant.now(),
                command.performedBy());
        contract.recordPickup(preInspection, ZonedDateTime.now());

        Vehicle vehicle = loadVehicle(contract.getVehicleId());
        vehicle.updateMileage(command.startMileage());

        contractRepository.save(contract);
        vehicleRepository.save(vehicle);
        publishEvents(contract.pullDomainEvents());
        publishEvents(vehicle.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("PICKUP_RECORDED", contract.getId(), command.performedBy()));
    }

    @Override
    public ReturnSummary recordReturn(RecordReturnCommand command) {
        Contract contract = loadContract(command.contractId());
        Vehicle vehicle = loadVehicle(contract.getVehicleId());
        Inspection postInspection = new Inspection(Inspection.InspectionType.POST, command.postInspection(),
                command.endFuelLevel(), command.endMileage(), command.photoKeys(), Instant.now(),
                command.performedBy());

        boolean hasDamage = contract.recordReturn(postInspection, ZonedDateTime.now());
        Reservation reservation = loadReservation(contract.getReservationId());
        vehicle.updateMileage(command.endMileage());
        vehicle.markAsAvailable();
        if (hasDamage && command.damageSeverity() == DamageSeverity.MAJOR) {
            vehicle.sendToMaintenance();
        }
        reservation.complete();

        Money lateFee = pricingService.calculateLateFee(contract.getScheduledReturn(),
                contract.getActualReturnDatetime(), LATE_FEE_HOURLY_RATE);
        Money fuelSurcharge = pricingService.calculateFuelSurcharge(contract.getPreInspection().fuelLevel(),
                command.endFuelLevel(), FUEL_RATE_PER_LITER, TANK_CAPACITY_LITERS);

        DamageReportId damageReportId = null;
        if (hasDamage && command.damageDescription() != null) {
            DamageReport report = DamageReport.report(vehicle.getId(), contract.getId(), contract.getCustomerId(),
                    command.damageDescription(), command.damageSeverity(), command.damageLiability(),
                    command.estimatedDamageCost());
            damageReportRepository.save(report);
            publishEvents(report.pullDomainEvents());
            damageReportId = report.getId();
        }

        cachePort.invalidate(vehicle.getId());
        contractRepository.save(contract);
        vehicleRepository.save(vehicle);
        reservationRepository.save(reservation);
        publishEvents(contract.pullDomainEvents());
        publishEvents(vehicle.pullDomainEvents());
        publishEvents(reservation.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("RETURN_RECORDED", contract.getId(), command.performedBy()));
        return new ReturnSummary(contract.getId(), hasDamage, damageReportId, lateFee, fuelSurcharge,
                lateFee.add(fuelSurcharge));
    }

    @Override
    public void extend(ExtendContractCommand command) {
        Contract contract = loadContract(command.contractId());
        Reservation reservation = loadReservation(contract.getReservationId());
        if (!command.newScheduledReturn().isAfter(contract.getScheduledReturn())) {
            contract.extend(command.newScheduledReturn());
        }
        DateRange extensionPeriod = new DateRange(contract.getScheduledReturn(), command.newScheduledReturn());
        if (!availabilityPort.isAvailable(contract.getVehicleId(), extensionPeriod)) {
            throw new VehicleNotAvailableException(contract.getVehicleId(), extensionPeriod);
        }

        contract.extend(command.newScheduledReturn());
        reservation.extend(command.newScheduledReturn());
        contractRepository.save(contract);
        reservationRepository.save(reservation);
        publishEvents(contract.pullDomainEvents());
        publishEvents(reservation.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("CONTRACT_EXTENDED", contract.getId(), command.extendedBy()));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDetail get(ContractId id) {
        return toDetail(loadContract(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractSummary> list(ListContractsQuery query) {
        return contractRepository.findAll(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummary> listActive() {
        return contractRepository.findActive();
    }

    @Override
    public String upload(UploadPhotoCommand command) {
        loadContract(command.contractId());
        String filename = UUID.randomUUID() + extension(command.originalFilename());
        return fileStoragePort.upload(command.fileBytes(), "contracts/" + command.contractId().value(), filename,
                command.contentType());
    }

    @Override
    public DamageReportId create(CreateDamageReportCommand command) {
        DamageReport report = DamageReport.report(command.vehicleId(), command.contractId(), command.customerId(),
                command.description(), command.severity(), command.liability(), command.estimatedCost());
        damageReportRepository.save(report);
        publishEvents(report.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("DAMAGE_REPORT_CREATED", report.getId(), command.reportedBy()));
        return report.getId();
    }

    @Override
    public String upload(UploadDamagePhotoCommand command) {
        DamageReport report = loadDamageReport(command.reportId());
        String filename = UUID.randomUUID() + extension(command.originalFilename());
        String key = fileStoragePort.upload(command.fileBytes(), "damage-reports/" + command.reportId().value(),
                filename, command.contentType());
        report.addPhoto(key);
        damageReportRepository.save(report);
        auditLogPort.log(AuditEntry.of("DAMAGE_PHOTO_UPLOADED", report.getId(), command.uploadedBy()));
        return key;
    }

    @Override
    @Transactional(readOnly = true)
    public DamageReportDetail get(DamageReportId id) {
        return toDetail(loadDamageReport(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DamageReportSummary> list(ListDamageReportsQuery query) {
        return damageReportRepository.findAll(query);
    }

    private Contract loadContract(ContractId id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id.value()));
    }

    private DamageReport loadDamageReport(DamageReportId id) {
        return damageReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Damage report not found: " + id.value()));
    }

    private Reservation loadReservation(com.rentflow.shared.id.ReservationId id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id.value()));
    }

    private Vehicle loadVehicle(com.rentflow.shared.id.VehicleId id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id.value()));
    }

    private void publishEvents(List<DomainEvent> events) {
        events.forEach(eventPublisher::publish);
    }

    private static ContractDetail toDetail(Contract contract) {
        return new ContractDetail(contract.getId(), contract.getContractNumber(), contract.getReservationId(),
                contract.getCustomerId(), contract.getVehicleId(), contract.getScheduledPickup(),
                contract.getScheduledReturn(), contract.getActualPickupDatetime(),
                contract.getActualReturnDatetime(), contract.getStatus(), contract.getPreInspection(),
                contract.getPostInspection(), contract.getSignatureKey());
    }

    private static DamageReportDetail toDetail(DamageReport report) {
        return new DamageReportDetail(report.getId(), report.getVehicleId(), report.getContractId(),
                report.getCustomerId(), report.getDamageDescription(), report.getSeverity(), report.getStatus(),
                report.getLiability(), report.getEstimatedCost(), report.getActualCost(), report.getPhotoKeys(),
                report.getReportedAt());
    }

    private static String extension(String filename) {
        int index = filename == null ? -1 : filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index);
    }
}
