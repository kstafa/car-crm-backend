package com.rentflow.reservation;

import com.rentflow.fleet.FuelLevel;
import com.rentflow.shared.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class ReservationPricingService {

    public Money calculateBaseAmount(Money dailyRate, DateRange period) {
        return dailyRate.multiply(BigDecimal.valueOf(period.durationInDays()));
    }

    public Money calculateLateFee(ZonedDateTime scheduledReturn, ZonedDateTime actualReturn, Money hourlyRate) {
        if (!actualReturn.isAfter(scheduledReturn)) {
            return Money.zero(hourlyRate.currency());
        }
        long hoursLate = ChronoUnit.HOURS.between(scheduledReturn, actualReturn);
        return hourlyRate.multiply(BigDecimal.valueOf(hoursLate));
    }

    public Money calculateFuelSurcharge(FuelLevel contracted, FuelLevel returned, Money ratePerLiter,
                                        int tankCapacityLiters) {
        int missing = contracted.percentageFull() - returned.percentageFull();
        if (missing <= 0) {
            return Money.zero(ratePerLiter.currency());
        }
        BigDecimal missingLiters = BigDecimal.valueOf(missing)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(tankCapacityLiters));
        return ratePerLiter.multiply(missingLiters);
    }
}
