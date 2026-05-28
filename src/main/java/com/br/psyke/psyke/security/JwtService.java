package com.br.psyke.psyke.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expMs;
    }

    public String generateAccessToken(TokenPayload p) {
        var now = Instant.now();
        return Jwts.builder().subject(p.userId().toString()).issuer("psyke")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expirationMs)))
                .claims(Map.of("tenant_id", p.tenantId().toString(), "clinic_id",
                    p.clinicId() != null ? p.clinicId().toString() : "",
                    "roles", p.roles(), "name", p.name(), "email", p.email()))
                .signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    public String generateRefreshToken(UUID userId) {
        var now = Instant.now();
        return Jwts.builder().subject(userId.toString()).issuer("psyke")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(604800000L)))
                .id(UUID.randomUUID().toString()).signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try { extractClaims(token); return true; }
        catch (ExpiredJwtException e) { log.warn("JWT expired"); }
        catch (JwtException e) { log.warn("JWT invalid"); }
        return false;
    }

    public UUID userId(String token) { return UUID.fromString(extractClaims(token).getSubject()); }
    public String tenantId(String token) { return extractClaims(token).get("tenant_id", String.class); }
    public String clinicId(String token) { return extractClaims(token).get("clinic_id", String.class); }
    @SuppressWarnings("unchecked")
    public List<String> roles(String token) { return extractClaims(token).get("roles", List.class); }
    public long expirationMs() { return expirationMs; }

    public record TokenPayload(UUID userId, UUID tenantId, UUID clinicId, String name, String email, List<String> roles) {}
}
