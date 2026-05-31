package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.exception.*;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.security.*;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.sql.SQLException;
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
    private final PasswordResetTokenRepository passwordResetTokens;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final TenantAwareDataSource ds;
    private final EmailService emailService;
    @Resource(name = "master")
    private DataSource master;

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

    public TenantResponse getTenant(UUID id) {
        var t = tenants.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return new TenantResponse(t.getId().toString(), t.getName(), t.getSlug(),
                t.getSchemaName(), t.getPlan().name(), t.getCreatedAt().toString());
    }

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        var user = users.findByEmailAndTenantId(req.email(), UUID.fromString(req.tenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var reset = passwordResetTokens.save(PasswordResetToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID().toString() + "-" + UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());

        var resetUrl = baseUrl + "/reset-password?token=" + reset.getToken();
        emailService.sendPasswordReset(user.getEmail(), user.getFullName(), resetUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        var reset = passwordResetTokens.findByToken(req.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!reset.isValid())
            throw new InvalidTokenException("Reset token has expired");

        var user = users.findById(reset.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(encoder.encode(req.newPassword()));
        reset.setUsedAt(OffsetDateTime.now());
    }

    @Transactional
    public void sendEmailVerification(UUID userId) {
        var user = users.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var vt = passwordResetTokens.save(PasswordResetToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString() + "-" + UUID.randomUUID())
                .type("VERIFY_EMAIL")
                .expiresAt(OffsetDateTime.now().plusHours(48))
                .build());

        var verifyUrl = baseUrl + "/auth/verify-email?token=" + vt.getToken();
        emailService.sendEmailVerification(user.getEmail(), user.getFullName(), verifyUrl);
    }

    @Transactional
    public void verifyEmail(String token) {
        var vt = passwordResetTokens.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));
        if (!"VERIFY_EMAIL".equals(vt.getType()))
            throw new InvalidTokenException("Invalid token type");
        if (!vt.isValid())
            throw new InvalidTokenException("Verification token has expired");

        var user = users.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEmailVerified(true);
        vt.setUsedAt(OffsetDateTime.now());
    }

    public UserResponse me(UUID id) {
        var u = users.findByIdWithRoles(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserResponse(u.getId().toString(), u.getFullName(), u.getEmail(), u.getCpf(), u.getPhone(),
                u.isActive(), u.isEmailVerified(), u.getRoles().stream().map(Role::getName).toList());
    }

    private String saveRefresh(UUID userId, String ip, String ua) {
        return refreshTokens.save(RefreshToken.builder().userId(userId)
                .token(jwt.generateRefreshToken(userId))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwt.refreshExpirationMs() / 1000))
                .sourceIp(ip).userAgent(ua).build()).getToken();
    }

    private void createAdmin(Tenant t, String email, String pass, String name) {
        var role = roles.findByName("ROLE_ADMIN").orElseGet(() ->
                roles.save(Role.builder().name("ROLE_ADMIN").description("Administrator").build()));
        var user = users.save(User.builder().tenantId(t.getId()).fullName(name).email(email)
                .passwordHash(encoder.encode(pass)).active(true).emailVerified(true)
                .roles(java.util.Set.of(role)).build());

        var schema = t.getSchemaName();
        try (var conn = master.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO " + schema + ".clinics (name, active) VALUES (?, ?) RETURNING id")) {
            stmt.setString(1, "Default Clinic");
            stmt.setBoolean(2, true);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                user.setClinicId(rs.getObject("id", UUID.class));
                users.save(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create default clinic", e);
        }
    }
}
