package com.rentflow.staff.adapter.out.persistence;

import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.id.StaffId;
import com.rentflow.staff.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JpaStaffDetailsService implements UserDetailsService {

    private final SpringDataStaffRepo staffRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        StaffJpaEntity staff = staffRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Staff not found: " + email));
        if (!"ACTIVE".equals(staff.status)) {
            throw new UsernameNotFoundException("Staff account is not active: " + email);
        }

        Set<String> permissions = staff.role.permissions.stream()
                .map(Permission::name)
                .collect(Collectors.toSet());

        return new StaffPrincipal(StaffId.of(staff.id), staff.email, staff.role.name, permissions,
                staff.passwordHash);
    }
}
