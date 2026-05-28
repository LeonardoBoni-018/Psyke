package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.exception.*;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final TenantRepository tenants;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final TenantAwareDataSource ds;
    private final DataSource master;

    @Value("${spring.datasource.url}")     private String url;
    @Value("${spring.datasource.username}") private String dbUser;
    @Value("${spring.datasource.password}") private String dbPass;

    @Transactional
    public TokenResponse login(LoginRequest r, String ip, String ua) {
        var user = users.findByEmailAndTenantId(r.email(), UUID.fromString(r.tenantId()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!user.isActive()) throw new InactiveUserException("User is inactive");
        if (!encoder.matches(r.password(), user.getPasswordHash()))
            throw new InvalidCredentialsException("Invalid email or password");

        user.setLastLogin(OffsetDateTime.now());
        var roleList = user.getRoles().stream().map(Role::getName).toList();
        var p = new JwtService.TokenPayload(user.getId(), user.getTenantId(), user.getClinicId(),
                user.getFullName(), user.getEmail(), roleList);
        return new TokenResponse(jwt.generateAccessToken(p), saveRefresh(user.getId(), ip, ua),
                "Bearer", jwt.expirationMs() / 1000, user.getId().toString(), user.getFullName(), roleList);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest r, String ip, String ua) {
        var rt = refreshTokens.findByToken(r.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        if (!rt.isValid()) {
            refreshTokens.revokeAllByUserId(rt.getUserId());
            throw new InvalidTokenException("Refresh token expired. Login again.");
        }
        rt.setRevoked(true);
        var user = users.findById(rt.getUserId()).orElseThrow(() -> new InvalidTokenException("User not found"));
        var roleList = user.getRoles().stream().map(Role::getName).toList();
        var p = new JwtService.TokenPayload(user.getId(), user.getTenantId(), user.getClinicId(),
                user.getFullName(), user.getEmail(), roleList);
        return new TokenResponse(jwt.generateAccessToken(p), saveRefresh(user.getId(), ip, ua),
                "Bearer", jwt.expirationMs() / 1000, user.getId().toString(), user.getFullName(), roleList);
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest r) {
        if (tenants.existsBySlug(r.slug()))
            throw new TenantAlreadyExistsException("Slug '" + r.slug() + "' already in use");

        var schema = "tenant_" + r.slug().replaceAll("[^a-z0-9_]", "_");
        var t = tenants.save(Tenant.builder().name(r.name()).slug(r.slug())
                .plan(Tenant.Plan.valueOf(r.plan().toUpperCase())).schemaName(schema)
                .maxClinics(r.maxClinics() != null ? r.maxClinics() : 1)
                .maxUsers(r.maxUsers() != null ? r.maxUsers() : 10).active(true).build());

        Flyway.configure().dataSource(master).schemas(schema).locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true).load().migrate();
        ds.addTenantDataSource(t.getId().toString(), schema, url, dbUser, dbPass);
        createAdmin(t, r.adminEmail(), r.adminPassword(), r.adminName());

        return new TenantResponse(t.getId().toString(), t.getName(), t.getSlug(),
                t.getSchemaName(), t.getPlan().name(), t.getCreatedAt().toString());
    }

    @Transactional
    public void logout(String token) {
        refreshTokens.findByToken(token).ifPresent(rt -> { rt.setRevoked(true); });
    }

    public UserResponse me(UUID id) {
        var u = users.findByIdWithRoles(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserResponse(u.getId().toString(), u.getFullName(), u.getEmail(), u.getCpf(), u.getPhone(),
                u.isActive(), u.isEmailVerified(), u.getRoles().stream().map(Role::getName).toList());
    }

    private String saveRefresh(UUID userId, String ip, String ua) {
        return refreshTokens.save(RefreshToken.builder().userId(userId)
                .token(jwt.generateRefreshToken(userId)).expiresAt(OffsetDateTime.now().plusDays(7))
                .sourceIp(ip).userAgent(ua).build()).getToken();
    }

    private void createAdmin(Tenant t, String email, String pass, String name) {
        var role = roles.findByName("ROLE_ADMIN").orElseGet(() ->
                roles.save(Role.builder().name("ROLE_ADMIN").description("Administrator").build()));
        users.save(User.builder().tenantId(t.getId()).fullName(name).email(email)
                .passwordHash(encoder.encode(pass)).active(true).emailVerified(true)
                .roles(java.util.Set.of(role)).build());
    }
}
