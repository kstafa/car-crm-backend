package com.rentflow.customer.port.out;

import com.rentflow.customer.Customer;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.query.ListCustomersQuery;
import com.rentflow.shared.id.CustomerId;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface CustomerRepository {
    void save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    boolean existsByEmail(String email);

    Page<CustomerSummary> findAll(ListCustomersQuery query);
}
