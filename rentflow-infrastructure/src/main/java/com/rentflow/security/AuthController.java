package com.rentflow.security;

import com.rentflow.shared.id.StaffId;
import com.rentflow.staff.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String ADMIN_EMAIL = "admin@rentflow.com";
    private static final String ADMIN_PASSWORD = "changeme";
    private static final StaffId ADMIN_STAFF_ID = StaffId.of("00000000-0000-0000-0000-000000000001");

    private final JwtTokenService jwtTokenService;

    public AuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        if (!ADMIN_EMAIL.equals(request.email()) || !ADMIN_PASSWORD.equals(request.password())) {
            return ResponseEntity.status(401).build();
        }
        Set<String> permissions = Arrays.stream(Permission.values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        StaffPrincipal principal = new StaffPrincipal(ADMIN_STAFF_ID, ADMIN_EMAIL, "ADMIN", permissions);
        return ResponseEntity.ok(new TokenResponse(jwtTokenService.generateAccessToken(principal),
                "static-refresh-token"));
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String refreshToken) {
    }
}
