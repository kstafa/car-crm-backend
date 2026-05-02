package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.Contract;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.port.out.ContractRepository;
import com.rentflow.contract.query.ListContractsQuery;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.ReservationId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaContractRepository implements ContractRepository {
    private final SpringDataContractRepo repo;
    private final ContractJpaMapper mapper;

    @Override
    @Transactional
    public void save(Contract contract) {
        ContractJpaEntity entity = mapper.toJpa(contract);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.createdBy = existing.createdBy;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<Contract> findById(ContractId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Contract> findByReservationId(ReservationId reservationId) {
        return repo.findByReservationId(reservationId.value()).map(mapper::toDomain);
    }

    @Override
    public Page<ContractSummary> findAll(ListContractsQuery query) {
        return repo.findFiltered(query.status(), query.vehicleId() == null ? null : query.vehicleId().value(),
                query.customerId() == null ? null : query.customerId().value(),
                PageRequest.of(query.page(), query.size())).map(mapper::toSummary);
    }

    @Override
    public List<ContractSummary> findActive() {
        return repo.findActive().stream().map(mapper::toSummary).toList();
    }
}
