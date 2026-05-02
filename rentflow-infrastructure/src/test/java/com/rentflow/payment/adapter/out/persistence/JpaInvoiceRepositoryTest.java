package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.payment.Invoice;
import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.query.ListInvoicesQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaInvoiceRepository.class, InvoiceJpaMapper.class})
class JpaInvoiceRepositoryTest extends AbstractJpaAdapterTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2027, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private JpaInvoiceRepository repo;
    @Autowired
    private SpringDataInvoiceRepo springRepo;
    @Autowired
    private InvoiceJpaMapper mapper;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void save_thenFindById_invoiceIsReconstituted() {
        Fixtures fixtures = insertFixtures("INV-JPA-001");
        Invoice invoice = invoice(fixtures, LocalDate.now().plusDays(7));

        repo.save(invoice);
        entityManager.flush();
        entityManager.clear();

        Optional<Invoice> found = repo.findById(invoice.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceNumber()).isEqualTo(invoice.getInvoiceNumber());
        assertThat(found.get().getContractId()).isEqualTo(fixtures.contractId());
    }

    @Test
    void save_withLineItemsAndPayments_persistsAll() {
        Invoice invoice = invoice(insertFixtures("INV-JPA-002"), LocalDate.now().plusDays(7));
        invoice.send();
        invoice.recordPayment(money("50.00"), PaymentMethod.CASH, null);

        repo.save(invoice);
        entityManager.flush();
        entityManager.clear();

        Optional<Invoice> found = repo.findById(invoice.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPayments()).hasSize(1);
        InvoiceDetail detail = mapper.toDetail(springRepo.findById(invoice.getId().value()).orElseThrow());
        assertThat(detail.lineItems()).hasSize(2);
    }

    @Test
    void findByContractId_existingContract_returnsInvoice() {
        Fixtures fixtures = insertFixtures("INV-JPA-003");
        Invoice invoice = invoice(fixtures, LocalDate.now().plusDays(7));
        repo.save(invoice);

        Optional<Invoice> found = repo.findByContractId(fixtures.contractId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(invoice.getId());
    }

    @Test
    void findByContractId_unknownContract_returnsEmpty() {
        assertThat(repo.findByContractId(ContractId.generate())).isEmpty();
    }

    @Test
    void findOverdue_sentInvoicePastDueDate_returnsIt() {
        Invoice invoice = invoice(insertFixtures("INV-JPA-004"), LocalDate.now().minusDays(1));
        invoice.send();
        repo.save(invoice);

        List<InvoiceSummary> overdue = repo.findOverdue();

        assertThat(overdue).extracting(InvoiceSummary::id).contains(invoice.getId());
    }

    @Test
    void findOverdue_paidInvoice_excluded() {
        Invoice invoice = invoice(insertFixtures("INV-JPA-005"), LocalDate.now().minusDays(1));
        invoice.recordPayment(money("290.00"), PaymentMethod.CARD, null);
        repo.save(invoice);

        assertThat(repo.findOverdue()).extracting(InvoiceSummary::id).doesNotContain(invoice.getId());
    }

    @Test
    void findOverdue_draftInvoice_excluded() {
        Invoice invoice = invoice(insertFixtures("INV-JPA-006"), LocalDate.now().minusDays(1));
        repo.save(invoice);

        assertThat(repo.findOverdue()).extracting(InvoiceSummary::id).doesNotContain(invoice.getId());
    }

    @Test
    void findFiltered_byStatus_returnsMatchingPage() {
        Invoice sent = invoice(insertFixtures("INV-JPA-007"), LocalDate.now().plusDays(7));
        sent.send();
        Invoice draft = invoice(insertFixtures("INV-JPA-008"), LocalDate.now().plusDays(7));
        repo.save(sent);
        repo.save(draft);

        Page<InvoiceSummary> result = repo.findAll(new ListInvoicesQuery(InvoiceStatus.SENT, null, null,
                null, null, 0, 20));

        assertThat(result.getContent()).extracting(InvoiceSummary::id).contains(sent.getId());
        assertThat(result.getContent()).extracting(InvoiceSummary::id).doesNotContain(draft.getId());
    }

    @Test
    void findFiltered_byCustomerId_returnsCustomerInvoices() {
        Fixtures firstFixtures = insertFixtures("INV-JPA-009");
        Invoice first = invoice(firstFixtures, LocalDate.now().plusDays(7));
        Invoice second = invoice(insertFixtures("INV-JPA-010"), LocalDate.now().plusDays(7));
        repo.save(first);
        repo.save(second);

        Page<InvoiceSummary> result = repo.findAll(new ListInvoicesQuery(null, firstFixtures.customerId(), null,
                null, null, 0, 20));

        assertThat(result.getContent()).extracting(InvoiceSummary::id).containsExactly(first.getId());
    }

    private Invoice invoice(Fixtures fixtures, LocalDate dueDate) {
        return Invoice.create(fixtures.contractId(), fixtures.customerId(), List.of(
                new LineItem("Rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1),
                new LineItem("Discount", InvoiceLineItemType.DISCOUNT, money("-10.00"), 1)
        ), dueDate);
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
