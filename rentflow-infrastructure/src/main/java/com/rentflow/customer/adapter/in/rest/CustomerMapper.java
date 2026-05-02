package com.rentflow.customer.adapter.in.rest;

import com.rentflow.customer.command.CreateCustomerCommand;
import com.rentflow.customer.model.CustomerDetail;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.shared.id.StaffId;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CreateCustomerCommand toCommand(CreateCustomerRequest request, StaffId staffId) {
        return new CreateCustomerCommand(request.firstName(), request.lastName(), request.email(), request.phone(),
                staffId);
    }

    public CustomerListResponse toListResponse(CustomerSummary summary) {
        return new CustomerListResponse(summary.id().value(), summary.firstName(), summary.lastName(),
                summary.email(), summary.phone(), summary.status().name());
    }

    public CustomerDetailResponse toDetailResponse(CustomerDetail detail) {
        return new CustomerDetailResponse(detail.id().value(), detail.firstName(), detail.lastName(), detail.email(),
                detail.phone(), detail.status().name(), detail.blacklistReason());
    }
}
