package com.rentflow.payment;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Deposit extends AggregateRoot {

    private final DepositId id;
    private final ContractId contractId;
    private final CustomerId customerId;
    private final InvoiceId invoiceId;
    private final Money amount;
    private DepositStatus status;
    private String releaseReason;
    private String forfeitReason;
    private final Instant heldAt;
    private Instant settledAt;

    private Deposit(DepositId id, ContractId contractId, CustomerId customerId, InvoiceId invoiceId, Money amount,
                    DepositStatus status, String releaseReason, String forfeitReason, Instant heldAt,
                    Instant settledAt) {
        this.id = id;
        this.contractId = contractId;
        this.customerId = customerId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.status = status;
        this.releaseReason = releaseReason;
        this.forfeitReason = forfeitReason;
        this.heldAt = heldAt;
        this.settledAt = settledAt;
    }

    public static Deposit hold(ContractId contractId, CustomerId customerId, InvoiceId invoiceId, Money amount) {
        Objects.requireNonNull(contractId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(invoiceId);
        Objects.requireNonNull(amount);
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Deposit amount must be positive");
        }
        Deposit deposit = new Deposit(DepositId.generate(), contractId, customerId, invoiceId, amount,
                DepositStatus.HELD, null, null, Instant.now(), null);
        deposit.registerEvent(new DepositHeldEvent(deposit.id, contractId, amount));
        return deposit;
    }

    public static Deposit reconstitute(DepositId id, ContractId contractId, CustomerId customerId,
                                       InvoiceId invoiceId, Money amount, DepositStatus status,
                                       String releaseReason, String forfeitReason, Instant heldAt,
                                       Instant settledAt) {
        return new Deposit(id, contractId, customerId, invoiceId, amount, status, releaseReason, forfeitReason,
                heldAt, settledAt);
    }

    public void release(String reason) {
        if (status != DepositStatus.HELD) {
            throw new InvalidStateTransitionException("Can only release a HELD deposit, current status: " + status);
        }
        releaseReason = reason;
        status = DepositStatus.RELEASED;
        settledAt = Instant.now();
        registerEvent(new DepositReleasedEvent(id, contractId, amount, reason));
    }

    public void forfeit(String reason) {
        if (status != DepositStatus.HELD) {
            throw new InvalidStateTransitionException("Can only forfeit a HELD deposit, current status: " + status);
        }
        forfeitReason = reason;
        status = DepositStatus.FORFEITED;
        settledAt = Instant.now();
        registerEvent(new DepositForfeitedEvent(id, contractId, amount, reason));
    }

    public DepositId getId() {
        return id;
    }

    public ContractId getContractId() {
        return contractId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
    }

    public Money getAmount() {
        return amount;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public String getForfeitReason() {
        return forfeitReason;
    }

    public Instant getHeldAt() {
        return heldAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }
}
