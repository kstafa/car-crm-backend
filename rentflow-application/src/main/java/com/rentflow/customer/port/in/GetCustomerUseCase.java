package com.rentflow.customer.port.in;

import com.rentflow.customer.model.CustomerDetail;
import com.rentflow.shared.id.CustomerId;

public interface GetCustomerUseCase {
    CustomerDetail get(CustomerId id);
}
