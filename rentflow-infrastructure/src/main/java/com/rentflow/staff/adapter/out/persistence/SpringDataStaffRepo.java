package com.rentflow.staff.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataStaffRepo extends JpaRepository<StaffJpaEntity, UUID> {
    Optional<StaffJpaEntity> findByEmail(String email);
}
