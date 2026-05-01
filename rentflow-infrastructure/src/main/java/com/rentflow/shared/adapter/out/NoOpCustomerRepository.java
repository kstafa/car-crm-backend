package com.rentflow.shared.adapter.out;

import com.rentflow.customer.Customer;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.shared.id.CustomerId;

import java.util.Optional;

public class NoOpCustomerRepository implements CustomerRepository {
    @Override
    public void save(Customer customer) {
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.empty();
    }
}
