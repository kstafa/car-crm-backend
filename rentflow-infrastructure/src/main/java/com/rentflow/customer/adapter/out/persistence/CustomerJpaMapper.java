package com.rentflow.customer.adapter.out.persistence;

import com.rentflow.customer.Customer;
import com.rentflow.customer.model.CustomerDetail;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.shared.id.CustomerId;
import org.springframework.stereotype.Component;

@Component
public class CustomerJpaMapper {

    public CustomerJpaEntity toJpa(Customer domain) {
        var entity = new CustomerJpaEntity();
        entity.id = domain.getId().value();
        entity.firstName = domain.getFirstName();
        entity.lastName = domain.getLastName();
        entity.email = domain.getEmail();
        entity.phone = domain.getPhone();
        entity.status = domain.getStatus();
        entity.blacklistReason = domain.getBlacklistReason();
        return entity;
    }

    public Customer toDomain(CustomerJpaEntity e) {
        return Customer.reconstitute(CustomerId.of(e.id), e.firstName, e.lastName, e.email, e.phone, e.status,
                e.blacklistReason);
    }

    public CustomerSummary toSummary(CustomerJpaEntity e) {
        return new CustomerSummary(CustomerId.of(e.id), e.firstName, e.lastName, e.email, e.phone, e.status);
    }

    public CustomerDetail toDetail(CustomerJpaEntity e) {
        return new CustomerDetail(CustomerId.of(e.id), e.firstName, e.lastName, e.email, e.phone, e.status,
                e.blacklistReason);
    }
}
