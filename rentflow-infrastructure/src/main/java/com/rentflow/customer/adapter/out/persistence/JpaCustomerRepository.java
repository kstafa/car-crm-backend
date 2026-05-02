package com.rentflow.customer.adapter.out.persistence;

import com.rentflow.customer.Customer;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.customer.query.ListCustomersQuery;
import com.rentflow.shared.id.CustomerId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class JpaCustomerRepository implements CustomerRepository {
    private final SpringDataCustomerRepo repo;
    private final CustomerJpaMapper mapper;

    @Override
    public void save(Customer customer) {
        CustomerJpaEntity entity = mapper.toJpa(customer);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    @Override
    public Page<CustomerSummary> findAll(ListCustomersQuery q) {
        PageRequest pageable = PageRequest.of(q.page(), q.size());
        return repo.findFiltered(q.status(), blankToNull(q.searchTerm()), pageable).map(mapper::toSummary);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
