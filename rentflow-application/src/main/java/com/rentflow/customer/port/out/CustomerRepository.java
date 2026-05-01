package com.rentflow.customer.port.out;

import com.rentflow.customer.Customer;
import com.rentflow.shared.id.CustomerId;

import java.util.Optional;

public interface CustomerRepository {
    void save(Customer customer);

    Optional<Customer> findById(CustomerId id);
}
