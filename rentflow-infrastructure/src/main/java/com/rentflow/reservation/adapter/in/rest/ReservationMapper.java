package com.rentflow.reservation.adapter.in.rest;

import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;
import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.reservation.model.CalendarEntry;
import com.rentflow.reservation.model.ConflictSummary;
import com.rentflow.reservation.model.ReservationDetail;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.query.GetCalendarQuery;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    public CreateReservationCommand toCommand(CreateReservationRequest req, StaffId staffId) {
        return new CreateReservationCommand(
                CustomerId.of(req.customerId()),
                VehicleId.of(req.vehicleId()),
                req.pickupDatetime(),
                req.returnDatetime(),
                staffId);
    }

    public ReservationListResponse toListResponse(ReservationSummary summary) {
        String customerName = customerRepository.findById(summary.customerId())
                .map(c -> c.getFirstName() + " " + c.getLastName())
                .orElse("Unknown");
        String licensePlate = vehicleRepository.findById(summary.vehicleId())
                .map(Vehicle::getLicensePlate)
                .orElse("Unknown");
        return new ReservationListResponse(
                summary.id().value(),
                summary.reservationNumber(),
                summary.customerId().value(),
                customerName,
                summary.vehicleId().value(),
                licensePlate,
                summary.pickupDatetime(),
                summary.returnDatetime(),
                summary.status().name(),
                summary.totalAmount().amount(),
                summary.totalAmount().currency().getCurrencyCode());
    }

    public ReservationDetailResponse toDetailResponse(ReservationDetail detail) {
        String customerName = customerRepository.findById(detail.customerId())
                .map(c -> c.getFirstName() + " " + c.getLastName())
                .orElse("Unknown");
        Vehicle vehicle = vehicleRepository.findById(detail.vehicleId()).orElse(null);
        String licensePlate = vehicle != null ? vehicle.getLicensePlate() : "Unknown";
        String brand = vehicle != null ? vehicle.getBrand() : "";
        String model = vehicle != null ? vehicle.getModel() : "";
        return new ReservationDetailResponse(
                detail.id().value(),
                detail.reservationNumber(),
                detail.customerId().value(),
                customerName,
                detail.vehicleId().value(),
                licensePlate,
                brand,
                model,
                detail.rentalPeriod().start(),
                detail.rentalPeriod().end(),
                detail.status().name(),
                detail.baseAmount().amount(),
                detail.discountAmount().amount(),
                detail.depositAmount().amount(),
                detail.taxAmount().amount(),
                detail.totalAmount().amount(),
                detail.baseAmount().currency().getCurrencyCode(),
                detail.notes(),
                List.of());
    }

    public CalendarEntryResponse toCalendarResponse(CalendarEntry entry) {
        return new CalendarEntryResponse(
                entry.reservationId().value(),
                entry.reservationNumber(),
                entry.vehicleId().value(),
                entry.vehicleLicensePlate(),
                entry.vehicleBrand(),
                entry.vehicleModel(),
                entry.customerId().value(),
                entry.customerName(),
                entry.pickupDatetime(),
                entry.returnDatetime(),
                entry.status().name());
    }

    public ConflictSummaryResponse toConflictResponse(ConflictSummary conflict) {
        return new ConflictSummaryResponse(
                conflict.draftReservationId().value(),
                conflict.draftReservationNumber(),
                conflict.vehicleId().value(),
                conflict.period().start(),
                conflict.period().end(),
                conflict.conflictingReservationId().value(),
                conflict.conflictingReservationNumber(),
                conflict.conflictingStatus().name());
    }

    public AvailableVehicleResponse toAvailableResponse(AvailableVehicle vehicle) {
        return new AvailableVehicleResponse(
                vehicle.id().value(),
                vehicle.licensePlate(),
                vehicle.brand(),
                vehicle.model(),
                vehicle.year(),
                vehicle.categoryName(),
                vehicle.dailyRate().amount(),
                vehicle.dailyRate().currency().getCurrencyCode());
    }

    public FindAvailableVehiclesQuery toAvailabilityQuery(UUID categoryId, ZonedDateTime pickup,
                                                           ZonedDateTime returnDate) {
        return new FindAvailableVehiclesQuery(VehicleCategoryId.of(categoryId), pickup, returnDate);
    }

    public GetCalendarQuery toCalendarQuery(LocalDate from, LocalDate to, UUID categoryId) {
        return new GetCalendarQuery(from, to, categoryId != null ? VehicleCategoryId.of(categoryId) : null);
    }

    public PageResponse<ReservationListResponse> toPageResponse(Page<ReservationSummary> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toListResponse).toList(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
