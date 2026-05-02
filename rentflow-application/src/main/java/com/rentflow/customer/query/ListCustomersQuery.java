package com.rentflow.customer.query;

import com.rentflow.customer.CustomerStatus;

public record ListCustomersQuery(CustomerStatus status, String searchTerm, int page, int size) {
}
