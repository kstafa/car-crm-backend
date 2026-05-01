package com.rentflow.security;

import com.rentflow.shared.id.StaffId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, AuthControllerTest.ProtectedController.class})
@Import({
        AuthController.class,
        AuthControllerTest.ProtectedController.class,
        SecurityConfig.class,
        JwtAuthFilter.class,
        JwtTokenService.class
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

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@rentflow.com","password":"changeme"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken").value("static-refresh-token"));
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@rentflow.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@rentflow.com","password":"changeme"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_validToken_returns200() throws Exception {
        StaffPrincipal principal = new StaffPrincipal(StaffId.generate(), "admin@rentflow.com", "ADMIN",
                Set.of("FLEET_VIEW"));
        String token = jwtTokenService.generateAccessToken(principal);

        mockMvc.perform(get("/api/v1/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void protectedEndpoint_expiredToken_returns401() throws Exception {
        JwtTokenService expiredService = new JwtTokenService("01234567890123456789012345678901", -1);
        StaffPrincipal principal = new StaffPrincipal(StaffId.generate(), "admin@rentflow.com", "ADMIN",
                Set.of("FLEET_VIEW"));
        String token = expiredService.generateAccessToken(principal);

        mockMvc.perform(get("/api/v1/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/v1/protected")
        public String protectedEndpoint() {
            return "ok";
        }
    }
}
