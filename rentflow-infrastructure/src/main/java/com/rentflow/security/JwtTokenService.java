package com.rentflow.security;

import com.rentflow.shared.id.StaffId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtTokenService {

    private final String secret;
    private final long accessTokenExpiryMs;

    public JwtTokenService(@Value("${rentflow.jwt.secret}") String secret,
                           @Value("${rentflow.jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs) {
        this.secret = secret;
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    public String generateAccessToken(StaffPrincipal principal) {
        Instant expiresAt = Instant.now().plusMillis(accessTokenExpiryMs);
        return Jwts.builder()
                .subject(principal.staffId().value().toString())
                .claim("email", principal.email())
                .claim("role", principal.role())
                .claim("permissions", List.copyOf(principal.permissions()))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public StaffPrincipal toPrincipal(Claims claims) {
        List<?> permissionClaims = claims.get("permissions", List.class);
        Set<String> permissions = new LinkedHashSet<>();
        if (permissionClaims != null) {
            permissionClaims.forEach(permission -> permissions.add(String.valueOf(permission)));
        }
        return new StaffPrincipal(
                StaffId.of(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class),
                permissions
        );
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
