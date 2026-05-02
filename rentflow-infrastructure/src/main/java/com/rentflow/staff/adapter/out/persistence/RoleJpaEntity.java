package com.rentflow.staff.adapter.out.persistence;

import com.rentflow.staff.Permission;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
class RoleJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false, length = 50)
    String name;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", length = 60)
    @Enumerated(EnumType.STRING)
    Set<Permission> permissions = new HashSet<>();
}
