package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.payment.Invoice;
import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.Refund;
import com.rentflow.payment.RefundReason;
import com.rentflow.payment.RefundStatus;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.query.ListRefundsQuery;
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

@Import({JpaRefundRepository.class, RefundJpaMapper.class, JpaInvoiceRepository.class, InvoiceJpaMapper.class})
class JpaRefundRepositoryTest extends AbstractJpaAdapterTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2027, 3, 10, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private JpaRefundRepository repo;
    @Autowired
    private JpaInvoiceRepository invoiceRepo;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void save_thenFindById_refundIsReconstituted() {
        Refund refund = refund("REF-JPA-001");

        repo.save(refund);

        Optional<Refund> found = repo.findById(refund.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getReason()).isEqualTo(RefundReason.GOODWILL);
    }

    @Test
    void findFiltered_byStatus_returnsPendingRefunds() {
        Refund pending = refund("REF-JPA-002");
        Refund approved = refund("REF-JPA-003");
        approved.approve(com.rentflow.shared.id.StaffId.generate());
        repo.save(pending);
        repo.save(approved);

        Page<RefundSummary> result = repo.findAll(new ListRefundsQuery(RefundStatus.PENDING, null, 0, 20));

        assertThat(result.getContent()).extracting(RefundSummary::id).contains(pending.getId());
        assertThat(result.getContent()).extracting(RefundSummary::id).doesNotContain(approved.getId());
    }

    private Refund refund(String licensePlate) {
        Fixtures fixtures = insertFixtures(licensePlate);
        Invoice invoice = Invoice.create(fixtures.contractId(), fixtures.customerId(),
                List.of(new LineItem("Rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1)),
                LocalDate.now().plusDays(7));
        invoice.recordPayment(money("300.00"), PaymentMethod.CARD, null);
        invoiceRepo.save(invoice);
        return Refund.request(invoice.getId(), fixtures.customerId(), money("100.00"), RefundReason.GOODWILL,
                PaymentMethod.CARD, "note");
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
