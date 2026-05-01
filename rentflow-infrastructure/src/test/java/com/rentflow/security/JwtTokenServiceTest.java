package com.rentflow.security;

import com.rentflow.shared.id.StaffId;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateAndParse_validPrincipal_roundTripsAllClaims() {
        JwtTokenService service = new JwtTokenService(SECRET, 900_000);
        StaffId staffId = StaffId.generate();
        StaffPrincipal principal = new StaffPrincipal(staffId, "admin@rentflow.com", "ADMIN",
                Set.of("FLEET_VIEW", "STAFF_MANAGE"));

        Claims claims = service.parseToken(service.generateAccessToken(principal));

        assertEquals(staffId.value().toString(), claims.getSubject());
        assertEquals("admin@rentflow.com", claims.get("email", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertTrue(claims.get("permissions", List.class).contains("FLEET_VIEW"));
        assertTrue(claims.get("permissions", List.class).contains("STAFF_MANAGE"));
    }

    @Test
    void parseToken_expiredToken_throwsException() {
        JwtTokenService expiredService = new JwtTokenService(SECRET, -1);
        StaffPrincipal principal = new StaffPrincipal(StaffId.generate(), "admin@rentflow.com", "ADMIN",
                Set.of("FLEET_VIEW"));
        String token = expiredService.generateAccessToken(principal);

        assertThrows(RuntimeException.class, () -> new JwtTokenService(SECRET, 900_000).parseToken(token));
    }

    @Test
    void parseToken_invalidSignature_throwsException() {
        JwtTokenService issuer = new JwtTokenService("abcdefghijklmnopqrstuvwxyz123456", 900_000);
        JwtTokenService parser = new JwtTokenService(SECRET, 900_000);
        String token = issuer.generateAccessToken(new StaffPrincipal(StaffId.generate(), "admin@rentflow.com",
                "ADMIN", Set.of("FLEET_VIEW")));

        assertThrows(RuntimeException.class, () -> parser.parseToken(token));
    }

    @Test
    void toPrincipal_claimsWithPermissions_mapsCorrectly() {
        StaffId staffId = StaffId.generate();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(staffId.value().toString());
        when(claims.get("email", String.class)).thenReturn("admin@rentflow.com");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(claims.get("permissions", List.class)).thenReturn(List.of("FLEET_VIEW", "STAFF_MANAGE"));

        StaffPrincipal principal = new JwtTokenService(SECRET, 900_000).toPrincipal(claims);

        assertEquals(staffId, principal.staffId());
        assertEquals("admin@rentflow.com", principal.email());
        assertEquals("ADMIN", principal.role());
        assertEquals(Set.of("FLEET_VIEW", "STAFF_MANAGE"), principal.permissions());
    }
}
