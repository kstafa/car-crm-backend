package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.Refund;
import com.rentflow.payment.RefundId;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.port.out.RefundRepository;
import com.rentflow.payment.query.ListRefundsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaRefundRepository implements RefundRepository {
    private final SpringDataRefundRepo repo;
    private final RefundJpaMapper mapper;

    @Override
    public void save(Refund refund) {
        RefundJpaEntity entity = mapper.toJpa(refund);
        repo.findById(entity.id).ifPresent(existing -> entity.version = existing.version);
        repo.save(entity);
    }

    @Override
    public Optional<Refund> findById(RefundId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Page<RefundSummary> findAll(ListRefundsQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size());
        return repo.findFiltered(query.status(), query.customerId() == null ? null : query.customerId().value(),
                pageable).map(mapper::toSummary);
    }
}
