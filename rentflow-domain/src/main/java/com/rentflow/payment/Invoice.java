package com.rentflow.payment;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Invoice extends AggregateRoot {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] INVOICE_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final InvoiceId id;
    private final String invoiceNumber;
    private final ContractId contractId;
    private final CustomerId customerId;
    private InvoiceStatus status;
    private final List<LineItem> lineItems;
    private Money paidAmount;
    private final LocalDate dueDate;

    private Invoice(InvoiceId id, String invoiceNumber, ContractId contractId, CustomerId customerId,
                    InvoiceStatus status, List<LineItem> lineItems, Money paidAmount, LocalDate dueDate) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.contractId = contractId;
        this.customerId = customerId;
        this.status = status;
        this.lineItems = lineItems;
        this.paidAmount = paidAmount;
        this.dueDate = dueDate;
    }

    public static Invoice create(ContractId contractId, CustomerId customerId, List<LineItem> lineItems,
                                 LocalDate dueDate) {
        Objects.requireNonNull(contractId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(lineItems);
        Objects.requireNonNull(dueDate);
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
        Money zero = Money.zero(lineItems.get(0).unitPrice().currency());
        return new Invoice(
                InvoiceId.generate(),
                "INV-" + randomInvoiceSuffix(),
                contractId,
                customerId,
                InvoiceStatus.DRAFT,
                new ArrayList<>(lineItems),
                zero,
                dueDate
        );
    }

    public void recordPayment(Money amount, PaymentMethod method, String reference) {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(method);
        if (status == InvoiceStatus.VOIDED) {
            throw new DomainException("Cannot record payment on voided invoice");
        }
        Money total = totalAmount();
        Money newPaid = paidAmount.add(amount);
        if (newPaid.isGreaterThan(total)) {
            throw new DomainException("Payment exceeds invoice total");
        }
        boolean wasPaid = status == InvoiceStatus.PAID;
        paidAmount = newPaid;
        status = paidAmount.equals(total) ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        registerEvent(new PaymentRecordedEvent(id, amount, method));
        if (!wasPaid && status == InvoiceStatus.PAID) {
            registerEvent(new InvoicePaidEvent(id, customerId, total));
        }
    }

    public void voidInvoice() {
        if (paidAmount.isGreaterThan(Money.zero(paidAmount.currency()))) {
            throw new DomainException("Cannot void invoice with prior payment");
        }
        status = InvoiceStatus.VOIDED;
    }

    public Money totalAmount() {
        Money total = Money.zero(lineItems.get(0).unitPrice().currency());
        for (LineItem lineItem : lineItems) {
            total = total.add(lineItem.total());
        }
        return total;
    }

    public Money outstandingAmount() {
        return totalAmount().subtract(paidAmount);
    }

    public InvoiceId getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public ContractId getContractId() {
        return contractId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public List<LineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public Money getPaidAmount() {
        return paidAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    private static String randomInvoiceSuffix() {
        StringBuilder value = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            value.append(INVOICE_NUMBER_CHARS[RANDOM.nextInt(INVOICE_NUMBER_CHARS.length)]);
        }
        return value.toString();
    }
}
