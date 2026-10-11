package com.br.psyke.psyke.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final String ISSUER = "psyke";
    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS = "access";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey secretKey;
    private final JwtParser parser;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpMs,
            @Value("${app.jwt.enforce-strong-secret:false}") boolean enforceStrongSecret) {
        var weak = secret.getBytes(StandardCharsets.UTF_8).length < 32 || secret.contains("CHANGE_ME");
        if (weak && enforceStrongSecret)
            throw new IllegalStateException("PSYKE_JWT_SECRET must be set to a random value with at least 32 bytes");
        if (weak)
            log.warn("Using a weak/default JWT secret. Set PSYKE_JWT_SECRET before deploying.");
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(secretKey).requireIssuer(ISSUER).clockSkewSeconds(30).build();
        this.expirationMs = expMs;
        this.refreshExpirationMs = refreshExpMs;
    }

    public String generateAccessToken(TokenPayload p) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(p.userId().toString())
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .id(UUID.randomUUID().toString())
                .claims(Map.of(
                    TOKEN_TYPE_CLAIM, ACCESS,
                    "tenant_id", p.tenantId().toString(),
                    "clinic_id", p.clinicId() != null ? p.clinicId().toString() : "",
                    "roles", p.roles(),
                    "name", p.name(),
                    "email", p.email()))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Opaque, unguessable refresh token (256 bits). Only its SHA-256 hash is persisted. */
    public String generateRefreshToken() {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Parses and validates an access token once. Empty when the token is invalid, expired or not an access token. */
    public Optional<AccessToken> parseAccessToken(String token) {
        try {
            var claims = parser.parseSignedClaims(token).getPayload();
            if (!ACCESS.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) return Optional.empty();
            var tenantId = claims.get("tenant_id", String.class);
            if (tenantId == null || tenantId.isBlank()) return Optional.empty();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            return Optional.of(new AccessToken(
                    UUID.fromString(claims.getSubject()),
                    tenantId,
                    claims.get("clinic_id", String.class),
                    roles != null ? roles : List.of()));
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT rejected: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public long expirationMs() { return expirationMs; }
    public long refreshExpirationMs() { return refreshExpirationMs; }

    public record TokenPayload(UUID userId, UUID tenantId, UUID clinicId,
                               String name, String email, List<String> roles) {}

    public record AccessToken(UUID userId, String tenantId, String clinicId, List<String> roles) {}
}
