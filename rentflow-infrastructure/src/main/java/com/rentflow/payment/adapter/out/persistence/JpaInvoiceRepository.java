package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.Invoice;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.port.out.InvoiceRepository;
import com.rentflow.payment.query.ListInvoicesQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class JpaInvoiceRepository implements InvoiceRepository {
    private final SpringDataInvoiceRepo repo;
    private final InvoiceJpaMapper mapper;

    @Override
    public void save(Invoice invoice) {
        InvoiceJpaEntity entity = mapper.toJpa(invoice);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.updatedAt = existing.updatedAt;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Page<InvoiceSummary> findAll(ListInvoicesQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size());
        return repo.findFiltered(query.status(), query.customerId() == null ? null : query.customerId().value(),
                query.contractId() == null ? null : query.contractId().value(), query.from(), query.to(), pageable)
                .map(mapper::toSummary);
    }

    @Override
    public Optional<Invoice> findByContractId(ContractId contractId) {
        return repo.findByContractId(contractId.value()).map(mapper::toDomain);
    }

    @Override
    public List<InvoiceSummary> findOverdue() {
        return repo.findOverdue(LocalDate.now()).stream().map(mapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceSummary> findByCustomerId(CustomerId customerId, Pageable pageable) {
        return repo.findByCustomerId(customerId.value(), pageable).map(mapper::toSummary);
    }
}
