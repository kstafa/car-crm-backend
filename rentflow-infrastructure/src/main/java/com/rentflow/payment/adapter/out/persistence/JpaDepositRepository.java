package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.Deposit;
import com.rentflow.payment.DepositId;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.port.out.DepositRepository;
import com.rentflow.payment.query.ListDepositsQuery;
import com.rentflow.shared.id.ContractId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaDepositRepository implements DepositRepository {
    private final SpringDataDepositRepo repo;
    private final DepositJpaMapper mapper;

    @Override
    public void save(Deposit deposit) {
        DepositJpaEntity entity = mapper.toJpa(deposit);
        repo.findById(entity.id).ifPresent(existing -> entity.version = existing.version);
        repo.save(entity);
    }

    @Override
    public Optional<Deposit> findById(DepositId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Deposit> findByContractId(ContractId contractId) {
        return repo.findByContractId(contractId.value()).map(mapper::toDomain);
    }

    @Override
    public Page<DepositSummary> findAll(ListDepositsQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size());
        return repo.findFiltered(query.status(), query.customerId() == null ? null : query.customerId().value(),
                pageable).map(mapper::toSummary);
    }
}
