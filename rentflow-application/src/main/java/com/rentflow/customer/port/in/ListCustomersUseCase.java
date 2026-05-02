package com.rentflow.customer.port.in;

import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.query.ListCustomersQuery;
import org.springframework.data.domain.Page;

public interface ListCustomersUseCase {
    Page<CustomerSummary> list(ListCustomersQuery q);
}
