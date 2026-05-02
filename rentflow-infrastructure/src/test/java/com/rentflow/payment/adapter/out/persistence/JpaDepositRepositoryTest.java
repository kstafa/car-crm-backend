package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.payment.Deposit;
import com.rentflow.payment.DepositStatus;
import com.rentflow.payment.Invoice;
import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.query.ListDepositsQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaDepositRepository.class, DepositJpaMapper.class, JpaInvoiceRepository.class, InvoiceJpaMapper.class})
class JpaDepositRepositoryTest extends AbstractJpaAdapterTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2027, 2, 10, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private JpaDepositRepository repo;
    @Autowired
    private JpaInvoiceRepository invoiceRepo;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void save_thenFindById_depositIsReconstituted() {
        Deposit deposit = deposit("DEP-JPA-001");

        repo.save(deposit);

        Optional<Deposit> found = repo.findById(deposit.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(deposit.getAmount());
    }

    @Test
    void findByContractId_existingContract_returnsDeposit() {
        Deposit deposit = deposit("DEP-JPA-002");
        repo.save(deposit);

        Optional<Deposit> found = repo.findByContractId(deposit.getContractId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(deposit.getId());
    }

    @Test
    void findFiltered_byStatus_returnsHeldDeposits() {
        Deposit held = deposit("DEP-JPA-003");
        Deposit released = deposit("DEP-JPA-004");
        released.release("completed");
        repo.save(held);
        repo.save(released);

        Page<DepositSummary> result = repo.findAll(new ListDepositsQuery(DepositStatus.HELD, null, 0, 20));

        assertThat(result.getContent()).extracting(DepositSummary::id).contains(held.getId());
        assertThat(result.getContent()).extracting(DepositSummary::id).doesNotContain(released.getId());
    }

    private Deposit deposit(String licensePlate) {
        Fixtures fixtures = insertFixtures(licensePlate);
        Invoice invoice = Invoice.create(fixtures.contractId(), fixtures.customerId(),
                List.of(new LineItem("Rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1)),
                LocalDate.now().plusDays(7));
        invoiceRepo.save(invoice);
        return Deposit.hold(fixtures.contractId(), fixtures.customerId(), invoice.getId(), money("300.00"));
    }

    private Fixtures insertFixtures(String licensePlate) {
        CustomerId customerId = CustomerId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO customers (id, first_name, last_name, email, status)
                VALUES (?, 'Ada', 'Lovelace', ?, 'ACTIVE')
                """, customerId.value(), customerId.value() + "@example.com");
        VehicleCategoryId categoryId = VehicleCategoryId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO vehicle_categories
                    (id, name, description, base_daily_rate, deposit_amount, currency, tax_rate)
                VALUES (?, ?, 'Test category', 49.99, 300.00, 'EUR', 0.20)
                """, categoryId.value(), "Category-" + categoryId.value());
        VehicleId vehicleId = VehicleId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO vehicles
                    (id, license_plate, brand, model, year, current_mileage, status, category_id, active)
                VALUES (?, ?, 'Toyota', 'Yaris', 2024, 1000, 'AVAILABLE', ?, TRUE)
                """, vehicleId.value(), licensePlate, categoryId.value());
        ReservationId reservationId = ReservationId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO reservations
                    (id, reservation_number, customer_id, vehicle_id, pickup_datetime, return_datetime, status,
                     base_amount, currency, discount_amount, deposit_amount, tax_amount)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 300.00, 'EUR', 0.00, 0.00, 0.00)
                """, reservationId.value(), "RES-" + licensePlate, customerId.value(), vehicleId.value(),
                OffsetDateTime.from(PICKUP), OffsetDateTime.from(PICKUP.plusDays(3)));
        ContractId contractId = ContractId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO contracts
                    (id, contract_number, reservation_id, customer_id, vehicle_id, scheduled_pickup,
                     scheduled_return, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """, contractId.value(), "CON-" + licensePlate, reservationId.value(), customerId.value(),
                vehicleId.value(), OffsetDateTime.from(PICKUP), OffsetDateTime.from(PICKUP.plusDays(3)));
        return new Fixtures(customerId, contractId);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    private record Fixtures(CustomerId customerId, ContractId contractId) {
    }
}
