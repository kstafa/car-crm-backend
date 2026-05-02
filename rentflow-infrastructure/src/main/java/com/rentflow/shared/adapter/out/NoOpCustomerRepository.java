package com.rentflow.shared.adapter.out;

import com.rentflow.customer.Customer;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.customer.query.ListCustomersQuery;
import com.rentflow.shared.id.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public class NoOpCustomerRepository implements CustomerRepository {
    @Override
    public void save(Customer customer) {
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.empty();
    }

    @Override
    public boolean existsByEmail(String email) {
        return false;
    }

    @Override
    public Page<CustomerSummary> findAll(ListCustomersQuery query) {
        int page = query == null ? 0 : Math.max(query.page(), 0);
        int size = query == null ? 20 : Math.max(query.size(), 1);
        return Page.empty(PageRequest.of(page, size));
    }
}
