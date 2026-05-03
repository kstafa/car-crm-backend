package com.rentflow.dashboard.adapter.out.persistence;

import com.rentflow.contract.ContractStatus;
import com.rentflow.contract.adapter.out.persistence.SpringDataContractRepo;
import com.rentflow.dashboard.model.DailyRevenue;
import com.rentflow.dashboard.model.UpcomingEvent;
import com.rentflow.dashboard.port.out.DashboardQueryPort;
import com.rentflow.fleet.VehicleStatus;
import com.rentflow.fleet.adapter.out.persistence.SpringDataVehicleRepo;
import com.rentflow.payment.adapter.out.persistence.SpringDataInvoiceRepo;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.shared.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaDashboardQueryAdapter implements DashboardQueryPort {

    private final ReservationRepository reservationRepository;
    private final SpringDataContractRepo contractRepo;
    private final SpringDataVehicleRepo vehicleRepo;
    private final SpringDataInvoiceRepo invoiceRepo;

    @Override
    public int countActiveRentalsToday() {
        return (int) contractRepo.countByStatus(ContractStatus.ACTIVE);
    }

    @Override
    public int countAvailableVehiclesNow() {
        return (int) vehicleRepo.countByStatusAndActiveTrue(VehicleStatus.AVAILABLE);
    }

    @Override
    public int countPickupsToday() {
        return reservationRepository.findTodayPickups().size();
    }

    @Override
    public int countReturnsToday() {
        return reservationRepository.findTodayReturns().size();
    }

    @Override
    public int countOverdueReturns() {
        return contractRepo.findOverdue(ZonedDateTime.now()).size();
    }

    @Override
    public Money revenueForMonth(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        BigDecimal total = invoiceRepo.sumPaidAmountBetween(from, to);
        return new Money(total != null ? total : BigDecimal.ZERO, Currency.getInstance("EUR"));
    }

    @Override
    public List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to) {
        return invoiceRepo.dailyRevenue(from, to).stream()
                .map(row -> new DailyRevenue(row.date(),
                        new Money(row.amount() != null ? row.amount() : BigDecimal.ZERO, Currency.getInstance("EUR"))))
                .toList();
    }

    @Override
    public List<UpcomingEvent> upcomingEvents(ZonedDateTime from, ZonedDateTime to) {
        List<UpcomingEvent> events = new ArrayList<>();
        for (ReservationSummary reservation : reservationRepository.findTodayPickups()) {
            events.add(new UpcomingEvent("PICKUP", reservation.id().value().toString(),
                    "Pickup: " + reservation.reservationNumber(), reservation.pickupDatetime()));
        }
        for (ReservationSummary reservation : reservationRepository.findTodayReturns()) {
            events.add(new UpcomingEvent("RETURN", reservation.id().value().toString(),
                    "Return: " + reservation.reservationNumber(), reservation.returnDatetime()));
        }
        events.sort(Comparator.comparing(UpcomingEvent::scheduledAt));
        return events;
    }
}
