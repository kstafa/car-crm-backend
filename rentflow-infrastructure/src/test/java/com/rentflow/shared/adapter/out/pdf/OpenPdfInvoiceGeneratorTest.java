package com.rentflow.shared.adapter.out.pdf;

import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.Payment;
import com.rentflow.payment.PaymentId;
import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class OpenPdfInvoiceGeneratorTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private final OpenPdfInvoiceGenerator generator = new OpenPdfInvoiceGenerator();

    @Test
    void generateInvoice_validDetail_returnsNonEmptyByteArray() {
        byte[] pdf = generator.generateInvoice(buildSampleInvoiceDetail());

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void generateInvoice_pdfStartsWithPdfMagicBytes() {
        byte[] pdf = generator.generateInvoice(buildSampleInvoiceDetail());

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateInvoice_withLateFeeLineItem_producesValidPdf() {
        assertThatNoException().isThrownBy(() -> generator.generateInvoice(buildInvoiceDetailWithLateFee()));
    }

    @Test
    void generateInvoice_emptyPaymentsList_producesValidPdf() {
        assertThatNoException().isThrownBy(() -> generator.generateInvoice(buildInvoiceDetailNoPayments()));
    }

    @Test
    void generateInvoice_withPaymentHistory_producesValidPdf() {
        assertThatNoException().isThrownBy(() -> generator.generateInvoice(buildInvoiceDetailWithPayments()));
    }

    private static InvoiceDetail buildSampleInvoiceDetail() {
        return buildInvoiceDetailWithPayments();
    }

    private static InvoiceDetail buildInvoiceDetailWithLateFee() {
        return detail(List.of(
                new LineItem("Rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1),
                new LineItem("Late return fee", InvoiceLineItemType.LATE_FEE, money("45.00"), 1)
        ), List.of());
    }

    private static InvoiceDetail buildInvoiceDetailNoPayments() {
        return detail(List.of(new LineItem("Rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1)),
                List.of());
    }

    private static InvoiceDetail buildInvoiceDetailWithPayments() {
        return detail(List.of(new LineItem("Rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1)),
                List.of(new Payment(PaymentId.generate(), money("300.00"), PaymentMethod.CARD, "ref",
                        Instant.now())));
    }

    private static InvoiceDetail detail(List<LineItem> items, List<Payment> payments) {
        Money total = items.stream().map(LineItem::total).reduce(Money.zero(EUR), Money::add);
        Money paid = payments.stream().map(Payment::amount).reduce(Money.zero(EUR), Money::add);
        return new InvoiceDetail(InvoiceId.generate(), "INV-TEST", ContractId.generate(), CustomerId.generate(),
                paid.equals(total) ? InvoiceStatus.PAID : InvoiceStatus.SENT, items, total, paid,
                total.subtract(paid), LocalDate.now(), LocalDate.now().plusDays(7), payments);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
