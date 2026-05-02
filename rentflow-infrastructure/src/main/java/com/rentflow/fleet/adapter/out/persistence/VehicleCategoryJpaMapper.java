package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.fleet.VehicleCategory;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class VehicleCategoryJpaMapper {

    public VehicleCategoryJpaEntity toJpa(VehicleCategory domain) {
        var entity = new VehicleCategoryJpaEntity();
        entity.id = domain.getId().value();
        entity.name = domain.getName();
        entity.description = domain.getDescription();
        entity.baseDailyRate = domain.getBaseDailyRate().amount();
        entity.depositAmount = domain.getDepositAmount().amount();
        entity.currency = domain.getBaseDailyRate().currency().getCurrencyCode();
        entity.taxRate = domain.getTaxRate();
        entity.active = domain.isActive();
        return entity;
    }

    public VehicleCategory toDomain(VehicleCategoryJpaEntity e) {
        Currency currency = Currency.getInstance(e.currency);
        return VehicleCategory.reconstitute(
                VehicleCategoryId.of(e.id),
                e.name,
                e.description,
                new Money(e.baseDailyRate, currency),
                new Money(e.depositAmount, currency),
                e.taxRate,
                e.active
        );
    }

    public CategorySummary toSummary(VehicleCategoryJpaEntity e) {
        Currency currency = Currency.getInstance(e.currency);
        return new CategorySummary(
                VehicleCategoryId.of(e.id),
                e.name,
                e.description,
                new Money(e.baseDailyRate, currency),
                new Money(e.depositAmount, currency),
                e.taxRate
        );
    }
}
