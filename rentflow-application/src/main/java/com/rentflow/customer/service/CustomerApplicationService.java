package com.rentflow.customer.service;

import com.rentflow.customer.Customer;
import com.rentflow.customer.command.BlacklistCustomerCommand;
import com.rentflow.customer.command.CreateCustomerCommand;
import com.rentflow.customer.command.ReactivateCustomerCommand;
import com.rentflow.customer.model.CustomerDetail;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.port.in.BlacklistCustomerUseCase;
import com.rentflow.customer.port.in.CreateCustomerUseCase;
import com.rentflow.customer.port.in.GetCustomerUseCase;
import com.rentflow.customer.port.in.ListCustomersUseCase;
import com.rentflow.customer.port.in.ReactivateCustomerUseCase;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.customer.query.ListCustomersQuery;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.port.out.AuditLogPort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerApplicationService implements CreateCustomerUseCase, GetCustomerUseCase, ListCustomersUseCase,
        BlacklistCustomerUseCase, ReactivateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final AuditLogPort auditLogPort;

    public CustomerApplicationService(CustomerRepository customerRepository, AuditLogPort auditLogPort) {
        this.customerRepository = customerRepository;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public CustomerId create(CreateCustomerCommand cmd) {
        if (customerRepository.existsByEmail(cmd.email())) {
            throw new DomainException("Email already in use");
        }

        Customer customer = Customer.create(CustomerId.generate(), cmd.firstName(), cmd.lastName(), cmd.email(),
                cmd.phone());
        customerRepository.save(customer);
        auditLogPort.log(AuditEntry.of("CREATE_CUSTOMER", customer.getId(), cmd.createdBy()));
        return customer.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetail get(CustomerId id) {
        Customer customer = loadCustomer(id);
        return toDetail(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummary> list(ListCustomersQuery q) {
        return customerRepository.findAll(q);
    }

    @Override
    public void blacklist(BlacklistCustomerCommand cmd) {
        Customer customer = loadCustomer(cmd.customerId());
        customer.blacklist(cmd.reason());
        customerRepository.save(customer);
        auditLogPort.log(AuditEntry.of("BLACKLIST_CUSTOMER", customer.getId(), cmd.performedBy()));
    }

    @Override
    public void reactivate(ReactivateCustomerCommand cmd) {
        Customer customer = loadCustomer(cmd.customerId());
        customer.reactivate();
        customerRepository.save(customer);
        auditLogPort.log(AuditEntry.of("REACTIVATE_CUSTOMER", customer.getId(), cmd.performedBy()));
    }

    private Customer loadCustomer(CustomerId id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id.value()));
    }

    private static CustomerDetail toDetail(Customer customer) {
        return new CustomerDetail(customer.getId(), customer.getFirstName(), customer.getLastName(),
                customer.getEmail(), customer.getPhone(), customer.getStatus(), customer.getBlacklistReason());
    }
}
