package com.rentflow.staff.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.staff.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(JpaStaffDetailsService.class)
class JpaStaffDetailsServiceTest extends AbstractJpaAdapterTest {

    private static final UUID ADMIN_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private JpaStaffDetailsService service;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void loadUserByUsername_existingActiveStaff_returnsPrincipalWithPermissions() {
        StaffPrincipal principal = (StaffPrincipal) service.loadUserByUsername("admin@rentflow.com");

        assertThat(principal.email()).isEqualTo("admin@rentflow.com");
        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(principal.permissions()).contains("FLEET_VIEW", "CUSTOMER_CREATE");
        assertThat(principal.getPassword()).isNotBlank();
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFoundException() {
        assertThatThrownBy(() -> service.loadUserByUsername("missing@rentflow.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_inactiveStaff_throwsUsernameNotFoundException() {
        StaffJpaEntity inactive = new StaffJpaEntity();
        inactive.id = UUID.randomUUID();
        inactive.email = "inactive@rentflow.com";
        inactive.passwordHash = "hash";
        inactive.firstName = "Inactive";
        inactive.lastName = "User";
        inactive.role = entityManager.find(RoleJpaEntity.class, ADMIN_ROLE_ID);
        inactive.status = "INACTIVE";
        entityManager.persistAndFlush(inactive);

        assertThatThrownBy(() -> service.loadUserByUsername("inactive@rentflow.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_adminRole_hasAllPermissions() {
        StaffPrincipal principal = (StaffPrincipal) service.loadUserByUsername("admin@rentflow.com");
        Set<String> expected = Arrays.stream(Permission.values()).map(Permission::name).collect(Collectors.toSet());

        assertThat(principal.permissions()).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(principal.permissions()).hasSize(29);
    }
}
