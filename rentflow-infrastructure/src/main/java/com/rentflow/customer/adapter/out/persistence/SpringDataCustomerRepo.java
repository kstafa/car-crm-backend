package com.rentflow.customer.adapter.out.persistence;

import com.rentflow.customer.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataCustomerRepo extends JpaRepository<CustomerJpaEntity, UUID> {
    boolean existsByEmail(String email);

    @Query("""
            SELECT c FROM CustomerJpaEntity c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:search IS NULL
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<CustomerJpaEntity> findFiltered(
            @Param("status") CustomerStatus status,
            @Param("search") String search,
            Pageable pageable);
}
