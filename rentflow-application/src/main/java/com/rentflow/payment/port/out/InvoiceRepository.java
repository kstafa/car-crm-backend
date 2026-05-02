package com.rentflow.payment.port.out;

import com.rentflow.payment.Invoice;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.query.ListInvoicesQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    void save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    Page<InvoiceSummary> findAll(ListInvoicesQuery query);

    Optional<Invoice> findByContractId(ContractId contractId);

    List<InvoiceSummary> findOverdue();

    Page<InvoiceSummary> findByCustomerId(CustomerId customerId, Pageable pageable);
}
