package com.rentflow.customer.service;

import com.rentflow.customer.Customer;
import com.rentflow.customer.CustomerStatus;
import com.rentflow.customer.command.BlacklistCustomerCommand;
import com.rentflow.customer.command.CreateCustomerCommand;
import com.rentflow.customer.command.ReactivateCustomerCommand;
import com.rentflow.customer.model.CustomerDetail;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.port.out.AuditLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AuditLogPort auditLogPort;

    private CustomerApplicationService service;
    private StaffId staffId;

    @BeforeEach
    void setUp() {
        service = new CustomerApplicationService(customerRepository, auditLogPort);
        staffId = StaffId.generate();
    }

    @Test
    void create_validParams_savesAndReturnsId() {
        when(customerRepository.existsByEmail("ada@example.com")).thenReturn(false);

        CustomerId id = service.create(createCommand());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(CustomerStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void create_duplicateEmail_throwsDomainException() {
        when(customerRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThrows(DomainException.class, () -> service.create(createCommand()));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_validParams_auditsAction() {
        when(customerRepository.existsByEmail("ada@example.com")).thenReturn(false);

        service.create(createCommand());

        verify(auditLogPort).log(any());
    }

    @Test
    void get_exists_returnsMappedDetail() {
        Customer customer = customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        CustomerDetail detail = service.get(customer.getId());

        assertEquals(customer.getId(), detail.id());
        assertEquals(customer.getEmail(), detail.email());
        assertEquals(customer.getPhone(), detail.phone());
    }

    @Test
    void get_notFound_throwsResourceNotFoundException() {
        CustomerId id = CustomerId.generate();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(id));
    }

    @Test
    void blacklist_activeCustomer_savesBlacklistedStatus() {
        Customer customer = customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        service.blacklist(new BlacklistCustomerCommand(customer.getId(), "fraud", staffId));

        assertEquals(CustomerStatus.BLACKLISTED, customer.getStatus());
        verify(customerRepository).save(customer);
    }

    @Test
    void blacklist_notFound_throwsResourceNotFoundException() {
        CustomerId id = CustomerId.generate();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.blacklist(new BlacklistCustomerCommand(id, "fraud", staffId)));
    }

    @Test
    void reactivate_blacklistedCustomer_savesActiveStatus() {
        Customer customer = customer();
        customer.blacklist("fraud");
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        service.reactivate(new ReactivateCustomerCommand(customer.getId(), staffId));

        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        verify(customerRepository).save(customer);
    }

    @Test
    void reactivate_notFound_throwsResourceNotFoundException() {
        CustomerId id = CustomerId.generate();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.reactivate(new ReactivateCustomerCommand(id, staffId)));
    }

    private CreateCustomerCommand createCommand() {
        return new CreateCustomerCommand("Ada", "Lovelace", "ada@example.com", "+33123456789", staffId);
    }

    private static Customer customer() {
        return Customer.create(CustomerId.generate(), "Ada", "Lovelace", "ada@example.com", "+33123456789");
    }
}
