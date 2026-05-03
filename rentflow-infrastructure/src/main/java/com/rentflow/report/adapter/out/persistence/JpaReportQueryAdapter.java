package com.rentflow.report.adapter.out.persistence;

import com.rentflow.contract.adapter.out.persistence.SpringDataContractRepo;
import com.rentflow.fleet.adapter.out.persistence.SpringDataVehicleRepo;
import com.rentflow.payment.adapter.out.persistence.SpringDataInvoiceRepo;
import com.rentflow.report.model.FleetUtilizationReport;
import com.rentflow.report.model.MonthlyRevenueRow;
import com.rentflow.report.model.RevenueReport;
import com.rentflow.report.model.VehicleUtilizationRow;
import com.rentflow.report.port.out.ReportQueryPort;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaReportQueryAdapter implements ReportQueryPort {

    private final SpringDataContractRepo contractRepo;
    private final SpringDataVehicleRepo vehicleRepo;
    private final SpringDataInvoiceRepo invoiceRepo;

    @Override
    public FleetUtilizationReport fleetUtilization(LocalDate from, LocalDate to) {
        int totalVehicles = (int) vehicleRepo.countByActiveTrue();
        int totalDays = (int) ChronoUnit.DAYS.between(from, to) + 1;
        List<VehicleUtilizationRow> rows = contractRepo.vehicleUtilization(
                        from.atStartOfDay(ZoneOffset.UTC),
                        to.plusDays(1).atStartOfDay(ZoneOffset.UTC))
                .stream()
                .map(row -> new VehicleUtilizationRow(
                        VehicleId.of(row.vehicleId()),
                        row.licensePlate(), row.brand(), row.model(),
                        Math.toIntExact(row.rentedDays()),
                        totalDays == 0 ? 0.0 : (row.rentedDays() * 100.0) / totalDays))
                .toList();
        int totalRentedDays = rows.stream().mapToInt(VehicleUtilizationRow::rentedDays).sum();
        double utilization = totalVehicles == 0 || totalDays == 0 ? 0.0
                : (totalRentedDays * 100.0) / ((long) totalVehicles * totalDays);
        return new FleetUtilizationReport(from, to, totalVehicles, totalDays, totalRentedDays, utilization, rows);
    }

    @Override
    public RevenueReport revenue(LocalDate from, LocalDate to) {
        Currency currency = Currency.getInstance("EUR");
        BigDecimal total = invoiceRepo.sumPaidAmountBetween(from, to);
        Money totalMoney = new Money(total != null ? total : BigDecimal.ZERO, currency);
        List<MonthlyRevenueRow> monthly = invoiceRepo.monthlyRevenue(from, to).stream()
                .map(row -> new MonthlyRevenueRow(row.month(),
                        new Money(row.amount() != null ? row.amount() : BigDecimal.ZERO, currency),
                        Math.toIntExact(row.rentalCount())))
                .toList();
        int completedRentals = monthly.stream().mapToInt(MonthlyRevenueRow::rentals).sum();
        Money average = completedRentals == 0 ? Money.zero(currency)
                : new Money(totalMoney.amount().divide(BigDecimal.valueOf(completedRentals), 2,
                RoundingMode.HALF_UP), currency);
        return new RevenueReport(from, to, totalMoney, average, completedRentals, monthly);
    }
}
