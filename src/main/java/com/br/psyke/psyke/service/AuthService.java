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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail, senha ou clínica inválidos";

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

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Lazily computed hash used to keep login timing constant when the user does not exist. */
    private volatile String dummyHash;

    @Transactional(noRollbackFor = {InvalidCredentialsException.class, InactiveUserException.class})
    public TokenResponse login(LoginRequest r, String ip, String ua) {
        var tenant = resolveTenant(r.tenantId()).filter(Tenant::isActive);
        var user = tenant.flatMap(t -> users.findByEmailIgnoreCaseAndTenantId(r.email().trim(), t.getId()));

        if (user.isEmpty()) {
            encoder.matches(r.password(), dummyHash()); // equalise response time (no user enumeration)
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }
        var u = user.get();
        if (!encoder.matches(r.password(), u.getPasswordHash()))
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        if (!u.isActive())
            throw new InactiveUserException("Usuário inativo. Fale com o administrador da clínica.");

        u.setLastLogin(OffsetDateTime.now());
        return issueTokens(u, tenant.get(), ip, ua);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public TokenResponse refresh(RefreshTokenRequest r, String ip, String ua) {
        var rt = refreshTokens.findByToken(TokenHasher.sha256(r.refreshToken()))
                .orElseThrow(() -> new InvalidTokenException("Sessão inválida. Entre novamente."));
        if (!rt.isValid()) {
            // A revoked token being replayed means it leaked: revoke the whole session family.
            if (rt.isRevoked()) refreshTokens.revokeAllByUserId(rt.getUserId());
            throw new InvalidTokenException("Sessão expirada. Entre novamente.");
        }
        rt.setRevoked(true);
        var user = users.findById(rt.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException("Sessão inválida. Entre novamente."));
        var tenant = tenants.findById(user.getTenantId())
                .filter(Tenant::isActive)
                .orElseThrow(() -> new InvalidTokenException("Clínica inativa."));
        return issueTokens(user, tenant, ip, ua);
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest r) {
        if (tenants.existsBySlug(r.slug()))
            throw new TenantAlreadyExistsException("Slug '" + r.slug() + "' already in use");

        var plan = Tenant.Plan.valueOf(r.plan().toUpperCase(Locale.ROOT));
        var schema = "tenant_" + r.slug().replaceAll("[^a-z0-9_]", "_");
        var t = tenants.save(Tenant.builder().name(r.name().trim()).slug(r.slug())
                .plan(plan).schemaName(schema)
                .maxClinics(plan == Tenant.Plan.BASIC ? 1 : 5)
                .maxUsers(switch (plan) { case BASIC -> 10; case PRO -> 50; case ENTERPRISE -> 500; })
                .active(true).build());

        Flyway.configure().dataSource(master).schemas(schema).locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true).load().migrate();
        ds.registerTenant(t.getId().toString(), schema);
        createAdmin(t, r.adminEmail().trim(), r.adminPassword(), r.adminName().trim());

        return toTenantResponse(t);
    }

    @Transactional
    public void logout(String token) {
        refreshTokens.findByToken(TokenHasher.sha256(token)).ifPresent(rt -> rt.setRevoked(true));
    }

    public TenantResponse getTenant(UUID id) {
        return tenants.findById(id).map(this::toTenantResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    /** Always succeeds from the caller's point of view, so it cannot be used to discover accounts. */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        var user = resolveTenant(req.tenantId())
                .flatMap(t -> users.findByEmailIgnoreCaseAndTenantId(req.email().trim(), t.getId()))
                .filter(User::isActive);
        if (user.isEmpty()) {
            log.info("Password reset requested for unknown account");
            return;
        }
        var raw = jwt.generateRefreshToken();
        passwordResetTokens.save(PasswordResetToken.builder()
                .userId(user.get().getId())
                .token(TokenHasher.sha256(raw))
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
        emailService.sendPasswordReset(user.get().getEmail(), user.get().getFullName(),
                frontendUrl + "/reset-password?token=" + raw);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        var reset = passwordResetTokens.findByToken(TokenHasher.sha256(req.token()))
                .filter(t -> "PASSWORD_RESET".equals(t.getType()))
                .orElseThrow(() -> new InvalidTokenException("Link inválido ou expirado"));
        if (!reset.isValid())
            throw new InvalidTokenException("Link inválido ou expirado");

        var user = users.findById(reset.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Link inválido ou expirado"));
        user.setPasswordHash(encoder.encode(req.newPassword()));
        reset.setUsedAt(OffsetDateTime.now());
        refreshTokens.revokeAllByUserId(user.getId()); // sign out every device
    }

    @Transactional
    public void sendEmailVerification(UUID userId) {
        var user = users.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var raw = jwt.generateRefreshToken();
        passwordResetTokens.save(PasswordResetToken.builder()
                .userId(userId)
                .token(TokenHasher.sha256(raw))
                .type("VERIFY_EMAIL")
                .expiresAt(OffsetDateTime.now().plusHours(48))
                .build());
        emailService.sendEmailVerification(user.getEmail(), user.getFullName(),
                frontendUrl + "/verify-email?token=" + raw);
    }

    @Transactional
    public void verifyEmail(String token) {
        var vt = passwordResetTokens.findByToken(TokenHasher.sha256(token))
                .filter(t -> "VERIFY_EMAIL".equals(t.getType()))
                .orElseThrow(() -> new InvalidTokenException("Link de verificação inválido"));
        if (!vt.isValid())
            throw new InvalidTokenException("Link de verificação expirado");

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

    private Optional<Tenant> resolveTenant(String idOrSlug) {
        var value = idOrSlug.trim();
        try {
            return tenants.findById(UUID.fromString(value));
        } catch (IllegalArgumentException notUuid) {
            return tenants.findBySlug(value.toLowerCase(Locale.ROOT));
        }
    }

    private TokenResponse issueTokens(User user, Tenant tenant, String ip, String ua) {
        var roleList = user.getRoles().stream().map(Role::getName).toList();
        var p = new JwtService.TokenPayload(user.getId(), user.getTenantId(), user.getClinicId(),
                user.getFullName(), user.getEmail(), roleList);
        var refresh = jwt.generateRefreshToken();
        refreshTokens.save(RefreshToken.builder().userId(user.getId())
                .token(TokenHasher.sha256(refresh))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwt.refreshExpirationMs() / 1000))
                .sourceIp(truncate(ip, 50)).userAgent(truncate(ua, 500)).build());
        return new TokenResponse(jwt.generateAccessToken(p), refresh, "Bearer", jwt.expirationMs() / 1000,
                user.getId().toString(), user.getFullName(), roleList, user.getEmail(),
                tenant.getId().toString(), tenant.getName(), tenant.getSlug());
    }

    private TenantResponse toTenantResponse(Tenant t) {
        return new TenantResponse(t.getId().toString(), t.getName(), t.getSlug(),
                t.getPlan().name(), t.getCreatedAt().toString());
    }

    private String dummyHash() {
        if (dummyHash == null) dummyHash = encoder.encode(UUID.randomUUID().toString());
        return dummyHash;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private void createAdmin(Tenant t, String email, String pass, String name) {
        var role = roles.findByName("ROLE_ADMIN").orElseGet(() ->
                roles.save(Role.builder().name("ROLE_ADMIN").description("Administrator").build()));
        var user = users.save(User.builder().tenantId(t.getId()).fullName(name).email(email)
                .passwordHash(encoder.encode(pass)).active(true).emailVerified(true)
                .roles(Set.of(role)).build());

        var schema = t.getSchemaName(); // validated: "tenant_" + [a-z0-9_]
        try (var conn = master.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO " + schema + ".clinics (name, active) VALUES (?, ?) RETURNING id")) {
            stmt.setString(1, t.getName());
            stmt.setBoolean(2, true);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                user.setClinicId(rs.getObject("id", UUID.class));
                users.save(user);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create default clinic", e);
        }
    }
}
