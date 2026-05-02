package com.rentflow.fleet;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

public final class VehicleCategory extends AggregateRoot {

    private final VehicleCategoryId id;
    private final String name;
    private final String description;
    private Money baseDailyRate;
    private Money depositAmount;
    private final BigDecimal taxRate;
    private boolean active;

    private VehicleCategory(VehicleCategoryId id, String name, String description, Money baseDailyRate,
                            Money depositAmount, BigDecimal taxRate, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseDailyRate = baseDailyRate;
        this.depositAmount = depositAmount;
        this.taxRate = taxRate;
        this.active = active;
    }

    public static VehicleCategory create(String name, String description, Money baseDailyRate,
                                         Money depositAmount, BigDecimal taxRate) {
        validateName(name);
        validateDailyRate(baseDailyRate);
        validateDepositAmount(depositAmount);
        validateTaxRate(taxRate);

        VehicleCategory category = new VehicleCategory(
                VehicleCategoryId.generate(),
                name,
                description,
                baseDailyRate,
                depositAmount,
                taxRate,
                true
        );
        category.registerEvent(new VehicleCategoryCreatedEvent(category.id, category.name));
        return category;
    }

    public static VehicleCategory reconstitute(VehicleCategoryId id, String name, String description,
                                               Money baseDailyRate, Money depositAmount, BigDecimal taxRate,
                                               boolean active) {
        return new VehicleCategory(id, name, description, baseDailyRate, depositAmount, taxRate, active);
    }

    public void deactivate() {
        active = false;
    }

    public void updateRates(Money newDailyRate, Money newDepositAmount) {
        validateDailyRate(newDailyRate);
        validateDepositAmount(newDepositAmount);
        baseDailyRate = newDailyRate;
        depositAmount = newDepositAmount;
    }

    public VehicleCategoryId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getBaseDailyRate() {
        return baseDailyRate;
    }

    public Money getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public boolean isActive() {
        return active;
    }

    private static void validateName(String name) {
        Objects.requireNonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    private static void validateDailyRate(Money dailyRate) {
        Objects.requireNonNull(dailyRate);
        if (dailyRate.amount().signum() <= 0) {
            throw new DomainException("baseDailyRate must be positive");
        }
    }

    private static void validateDepositAmount(Money depositAmount) {
        Objects.requireNonNull(depositAmount);
        if (depositAmount.amount().signum() < 0) {
            throw new DomainException("depositAmount must be non-negative");
        }
    }

    private static void validateTaxRate(BigDecimal taxRate) {
        Objects.requireNonNull(taxRate);
        if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new DomainException("taxRate must be between 0 and 1 inclusive");
        }
    }
}
