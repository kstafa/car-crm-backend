package com.rentflow.fleet.adapter.in.rest;

import com.rentflow.fleet.command.CreateCategoryCommand;
import com.rentflow.fleet.command.RegisterVehicleCommand;
import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.fleet.model.VehicleDetail;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class FleetMapper {

    private static final Currency EUR = Currency.getInstance("EUR");

    public RegisterVehicleCommand toCommand(RegisterVehicleRequest req, StaffId staffId) {
        return new RegisterVehicleCommand(req.licensePlate(), req.brand(), req.model(), req.year(),
                VehicleCategoryId.of(req.categoryId()), req.initialMileage(), req.description(), staffId);
    }

    public CreateCategoryCommand toCommand(CreateCategoryRequest req, StaffId staffId) {
        return new CreateCategoryCommand(req.name(), req.description(), new Money(req.baseDailyRate(), EUR),
                new Money(req.depositAmount(), EUR), req.taxRate(), staffId);
    }

    public FindAvailableVehiclesQuery toQuery(AvailabilityRequest req) {
        return new FindAvailableVehiclesQuery(VehicleCategoryId.of(req.categoryId()), req.pickupDatetime(),
                req.returnDatetime());
    }

    public VehicleListResponse toListResponse(VehicleSummary summary) {
        return new VehicleListResponse(summary.id().value(), summary.licensePlate(), summary.brand(), summary.model(),
                summary.year(), summary.status().name(), summary.categoryName(), summary.currentMileage(),
                summary.thumbnailKey());
    }

    public VehicleDetailResponse toDetailResponse(VehicleDetail detail) {
        return new VehicleDetailResponse(detail.id().value(), detail.licensePlate(), detail.brand(), detail.model(),
                detail.year(), detail.status().name(), detail.categoryId().value(), detail.categoryName(),
                detail.currentMileage(), detail.active(), detail.description(), detail.photoKeys());
    }

    public AvailableVehicleResponse toAvailableResponse(AvailableVehicle av) {
        return new AvailableVehicleResponse(av.id().value(), av.licensePlate(), av.brand(), av.model(), av.year(),
                av.categoryName(), av.dailyRate().amount(), av.thumbnailKey());
    }

    public CategoryResponse toResponse(CategorySummary summary) {
        return new CategoryResponse(summary.id().value(), summary.name(), summary.description(),
                summary.baseDailyRate().amount(), summary.depositAmount().amount(), summary.taxRate());
    }
}
