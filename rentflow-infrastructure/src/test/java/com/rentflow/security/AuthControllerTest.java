package com.rentflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.id.StaffId;
import com.rentflow.staff.Permission;
import com.rentflow.staff.adapter.out.persistence.JpaStaffDetailsService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        AuthController.class,
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtTokenService.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "rentflow.jwt.secret=01234567890123456789012345678901",
        "rentflow.jwt.access-token-expiry-ms=900000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JpaStaffDetailsService staffDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void login_validCredentials_returns200WithAccessToken() throws Exception {
        StaffPrincipal principal = principal(Set.of("FLEET_VIEW"));
        when(staffDetailsService.loadUserByUsername("admin@rentflow.com")).thenReturn(principal);
        when(passwordEncoder.matches("changeme", "hash")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@rentflow.com","password":"changeme"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-placeholder"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        StaffPrincipal principal = principal(Set.of("FLEET_VIEW"));
        when(staffDetailsService.loadUserByUsername("admin@rentflow.com")).thenReturn(principal);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@rentflow.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        when(staffDetailsService.loadUserByUsername("unknown@rentflow.com"))
                .thenThrow(new UsernameNotFoundException("missing"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@rentflow.com","password":"changeme"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_accessTokenIsValidJwt_containsRoleAndPermissions() throws Exception {
        Set<String> permissions = Arrays.stream(Permission.values())
                .map(Permission::name)
                .collect(Collectors.toUnmodifiableSet());
        StaffPrincipal principal = principal(permissions);
        when(staffDetailsService.loadUserByUsername("admin@rentflow.com")).thenReturn(principal);
        when(passwordEncoder.matches("changeme", "hash")).thenReturn(true);

        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@rentflow.com","password":"changeme"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readValue(json, Map.class).get("accessToken").toString();

        Claims claims = jwtTokenService.parseToken(token);

        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("admin@rentflow.com", claims.get("email", String.class));
        assertEquals(29, claims.get("permissions", java.util.List.class).size());
        assertTrue(claims.get("permissions", java.util.List.class).contains("FLEET_VIEW"));
    }

    private static StaffPrincipal principal(Set<String> permissions) {
        return new StaffPrincipal(StaffId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "admin@rentflow.com",
                "ADMIN", permissions, "hash");
    }
}
