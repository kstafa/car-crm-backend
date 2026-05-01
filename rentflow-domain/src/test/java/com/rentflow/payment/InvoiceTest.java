package com.rentflow.payment;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void create_validLineItems_setsDraftStatus() {
        assertEquals(InvoiceStatus.DRAFT, invoice().getStatus());
    }

    @Test
    void totalAmount_multipleLineItems_returnsSum() {
        assertEquals(money("25.00"), invoice().totalAmount());
    }

    @Test
    void outstandingAmount_noPayments_equalsTotal() {
        Invoice invoice = invoice();

        assertEquals(invoice.totalAmount(), invoice.outstandingAmount());
    }

    @Test
    void recordPayment_partialAmount_setsPartiallyPaid() {
        Invoice invoice = invoice();

        invoice.recordPayment(money("10.00"), PaymentMethod.CARD, "card-ref");

        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
    }

    @Test
    void recordPayment_fullAmount_setsPaidAndRegistersBothEvents() {
        Invoice invoice = invoice();

        invoice.recordPayment(money("25.00"), PaymentMethod.CARD, "card-ref");

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(2, invoice.pullDomainEvents().size());
    }

    @Test
    void recordPayment_exceedsTotal_throwsDomainException() {
        Invoice invoice = invoice();

        assertThrows(DomainException.class, () -> invoice.recordPayment(money("25.01"), PaymentMethod.CARD, "card-ref"));
    }

    @Test
    void recordPayment_onVoidedInvoice_throwsDomainException() {
        Invoice invoice = invoice();
        invoice.voidInvoice();

        assertThrows(DomainException.class, () -> invoice.recordPayment(money("1.00"), PaymentMethod.CASH, "cash"));
    }

    @Test
    void voidInvoice_noPriorPayments_setsVoided() {
        Invoice invoice = invoice();

        invoice.voidInvoice();

        assertEquals(InvoiceStatus.VOIDED, invoice.getStatus());
    }

    @Test
    void voidInvoice_withPriorPayment_throwsDomainException() {
        Invoice invoice = invoice();
        invoice.recordPayment(money("1.00"), PaymentMethod.CASH, "cash");

        assertThrows(DomainException.class, invoice::voidInvoice);
    }

    @Test
    void pullDomainEvents_afterFullPayment_returnsTwoEvents() {
        Invoice invoice = invoice();
        invoice.recordPayment(money("25.00"), PaymentMethod.CARD, "card-ref");

        List<?> events = invoice.pullDomainEvents();

        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof PaymentRecordedEvent);
        assertTrue(events.get(1) instanceof InvoicePaidEvent);
    }

    private static Invoice invoice() {
        return Invoice.create(ContractId.generate(), CustomerId.generate(), List.of(
                new LineItem("rental", money("10.00"), 2),
                new LineItem("fee", money("5.00"), 1)
        ), LocalDate.now().plusDays(7));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
