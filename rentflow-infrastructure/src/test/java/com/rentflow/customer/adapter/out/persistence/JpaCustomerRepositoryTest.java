package com.rentflow.customer.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.customer.Customer;
import com.rentflow.customer.CustomerStatus;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.query.ListCustomersQuery;
import com.rentflow.shared.id.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaCustomerRepository.class, CustomerJpaMapper.class})
class JpaCustomerRepositoryTest extends AbstractJpaAdapterTest {

    @Autowired
    private JpaCustomerRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_thenFindById_domainAggregateIsReconstituted() {
        Customer customer = customer("ada@example.com", "Ada", "Lovelace");

        repository.save(customer);
        entityManager.flush();
        entityManager.clear();

        Optional<Customer> found = repository.findById(customer.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("ada@example.com");
        assertThat(found.get().getPhone()).isEqualTo("+33123456789");
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        repository.save(customer("ada@example.com", "Ada", "Lovelace"));

        assertThat(repository.existsByEmail("ada@example.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(repository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void findAll_filterByStatus_returnsMatchingCustomers() {
        Customer active = customer("ada@example.com", "Ada", "Lovelace");
        Customer blacklisted = customer("grace@example.com", "Grace", "Hopper");
        blacklisted.blacklist("fraud");
        repository.save(active);
        repository.save(blacklisted);

        Page<CustomerSummary> result = repository.findAll(new ListCustomersQuery(CustomerStatus.BLACKLISTED, null,
                0, 20));

        assertThat(result.getContent()).extracting(CustomerSummary::id).containsExactly(blacklisted.getId());
    }

    @Test
    void findAll_searchByName_returnsMatchingCustomers() {
        Customer ada = customer("ada@example.com", "Ada", "Lovelace");
        Customer grace = customer("grace@example.com", "Grace", "Hopper");
        repository.save(ada);
        repository.save(grace);

        Page<CustomerSummary> result = repository.findAll(new ListCustomersQuery(null, "lovelace", 0, 20));

        assertThat(result.getContent()).extracting(CustomerSummary::id).containsExactly(ada.getId());
    }

    @Test
    void findAll_searchByEmail_returnsMatchingCustomers() {
        Customer ada = customer("ada@example.com", "Ada", "Lovelace");
        Customer grace = customer("grace@example.com", "Grace", "Hopper");
        repository.save(ada);
        repository.save(grace);

        Page<CustomerSummary> result = repository.findAll(new ListCustomersQuery(null, "grace@example.com", 0,
                20));

        assertThat(result.getContent()).extracting(CustomerSummary::id).containsExactly(grace.getId());
    }

    private static Customer customer(String email, String firstName, String lastName) {
        return Customer.create(CustomerId.generate(), firstName, lastName, email, "+33123456789");
    }
}
