package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.ForgotPasswordRequest;
import com.br.psyke.psyke.dto.LoginRequest;
import com.br.psyke.psyke.dto.RefreshTokenRequest;
import com.br.psyke.psyke.dto.ResetPasswordRequest;
import com.br.psyke.psyke.exception.InactiveUserException;
import com.br.psyke.psyke.exception.InvalidCredentialsException;
import com.br.psyke.psyke.exception.InvalidTokenException;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.security.JwtService;
import com.br.psyke.psyke.security.TokenHasher;
import com.br.psyke.psyke.service.AuthService;
import com.br.psyke.psyke.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository users;
    @Mock private TenantRepository tenants;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private PasswordResetTokenRepository passwordResetTokens;
    @Mock private EmailService emailService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-test-secret-12345",
            900_000, 604_800_000, true);
    private AuthService authService;
    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, tenants, null, refreshTokens, passwordResetTokens, jwt, encoder, null, emailService);
        ReflectionTestUtils.setField(authService, "frontendUrl", "https://app.psyke.test");
        tenant = Tenant.builder().id(UUID.randomUUID()).name("Clínica Serenity").slug("serenity")
                .schemaName("tenant_serenity").plan(Tenant.Plan.BASIC).active(true).build();
        user = User.builder().id(UUID.randomUUID()).tenantId(tenant.getId())
                .fullName("Ana Admin").email("ana@serenity.com")
                .passwordHash(encoder.encode("Correct-Horse-1"))
                .active(true).emailVerified(true)
                .roles(Set.of(Role.builder().name("ROLE_ADMIN").build())).build();
    }

    // ---- login -------------------------------------------------------------------------------

    @Test
    void shouldLoginWithClinicSlugAndStoreOnlyRefreshTokenHash() {
        when(tenants.findBySlug("serenity")).thenReturn(Optional.of(tenant));
        when(users.findByEmailIgnoreCaseAndTenantId("ana@serenity.com", tenant.getId())).thenReturn(Optional.of(user));
        when(refreshTokens.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = authService.login(new LoginRequest("ana@serenity.com", "Correct-Horse-1", "Serenity"), "1.2.3.4", "ua");

        assertThat(resp.accessToken()).isNotBlank();
        assertThat(resp.tenantId()).isEqualTo(tenant.getId().toString());
        assertThat(resp.tenantName()).isEqualTo("Clínica Serenity");
        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo(TokenHasher.sha256(resp.refreshToken()));
        assertThat(saved.getValue().getToken()).isNotEqualTo(resp.refreshToken());
        assertThat(jwt.parseAccessToken(resp.accessToken())).isPresent();
    }

    @Test
    void shouldLoginWithTenantUuid() {
        when(tenants.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(users.findByEmailIgnoreCaseAndTenantId("ana@serenity.com", tenant.getId())).thenReturn(Optional.of(user));
        when(refreshTokens.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = authService.login(new LoginRequest("ana@serenity.com", "Correct-Horse-1", tenant.getId().toString()), null, null);
        assertThat(resp.userId()).isEqualTo(user.getId().toString());
    }

    @Test
    void shouldRejectWrongPasswordWithGenericMessage() {
        when(tenants.findBySlug("serenity")).thenReturn(Optional.of(tenant));
        when(users.findByEmailIgnoreCaseAndTenantId(any(), any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@serenity.com", "wrong-pass", "serenity"), null, null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail, senha ou clínica inválidos");
    }

    @Test
    void shouldGiveSameErrorForUnknownClinicOrUser() {
        when(tenants.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("x@y.com", "whatever-1", "nope"), null, null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail, senha ou clínica inválidos");
    }

    @Test
    void shouldNotRevealInactiveUserBeforePasswordCheck() {
        user.setActive(false);
        when(tenants.findBySlug("serenity")).thenReturn(Optional.of(tenant));
        when(users.findByEmailIgnoreCaseAndTenantId(any(), any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@serenity.com", "wrong-pass", "serenity"), null, null))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@serenity.com", "Correct-Horse-1", "serenity"), null, null))
                .isInstanceOf(InactiveUserException.class);
    }

    // ---- refresh -----------------------------------------------------------------------------

    @Test
    void shouldRotateRefreshToken() {
        var raw = jwt.generateRefreshToken();
        var stored = RefreshToken.builder().userId(user.getId()).token(TokenHasher.sha256(raw))
                .expiresAt(OffsetDateTime.now().plusDays(1)).build();
        when(refreshTokens.findByToken(TokenHasher.sha256(raw))).thenReturn(Optional.of(stored));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(tenants.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(refreshTokens.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = authService.refresh(new RefreshTokenRequest(raw), null, null);

        assertThat(stored.isRevoked()).isTrue();
        assertThat(resp.refreshToken()).isNotEqualTo(raw);
    }

    @Test
    void shouldRevokeAllSessionsWhenRevokedTokenIsReplayed() {
        var raw = jwt.generateRefreshToken();
        var stored = RefreshToken.builder().userId(user.getId()).token(TokenHasher.sha256(raw))
                .expiresAt(OffsetDateTime.now().plusDays(1)).revoked(true).build();
        when(refreshTokens.findByToken(TokenHasher.sha256(raw))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(raw), null, null))
                .isInstanceOf(InvalidTokenException.class);
        verify(refreshTokens).revokeAllByUserId(user.getId());
    }

    // ---- password reset ------------------------------------------------------------------------

    @Test
    void shouldSendResetLinkToFrontendAndStoreHash() {
        when(tenants.findBySlug("serenity")).thenReturn(Optional.of(tenant));
        when(users.findByEmailIgnoreCaseAndTenantId("ana@serenity.com", tenant.getId())).thenReturn(Optional.of(user));
        when(passwordResetTokens.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.forgotPassword(new ForgotPasswordRequest("ana@serenity.com", "serenity"));

        var url = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(eq("ana@serenity.com"), eq("Ana Admin"), url.capture());
        assertThat(url.getValue()).startsWith("https://app.psyke.test/reset-password?token=");
        var rawToken = url.getValue().substring(url.getValue().indexOf('=') + 1);
        var saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokens).save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo(TokenHasher.sha256(rawToken));
    }

    @Test
    void shouldSilentlyIgnoreForgotPasswordForUnknownAccount() {
        when(tenants.findBySlug("serenity")).thenReturn(Optional.of(tenant));
        when(users.findByEmailIgnoreCaseAndTenantId(any(), any())).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("ghost@serenity.com", "serenity"));

        verifyNoInteractions(emailService);
        verify(passwordResetTokens, never()).save(any());
    }

    @Test
    void shouldResetPasswordAndRevokeSessions() {
        var raw = "reset-token-raw";
        var token = PasswordResetToken.builder().userId(user.getId()).token(TokenHasher.sha256(raw))
                .type("PASSWORD_RESET").expiresAt(OffsetDateTime.now().plusHours(1)).build();
        when(passwordResetTokens.findByToken(TokenHasher.sha256(raw))).thenReturn(Optional.of(token));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        authService.resetPassword(new ResetPasswordRequest(raw, "New-Password-9"));

        assertThat(encoder.matches("New-Password-9", user.getPasswordHash())).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        verify(refreshTokens).revokeAllByUserId(user.getId());
    }

    @Test
    void shouldRejectExpiredOrUnknownResetToken() {
        var expired = PasswordResetToken.builder().userId(user.getId()).token(TokenHasher.sha256("old"))
                .type("PASSWORD_RESET").expiresAt(OffsetDateTime.now().minusMinutes(1)).build();
        when(passwordResetTokens.findByToken(TokenHasher.sha256("old"))).thenReturn(Optional.of(expired));
        when(passwordResetTokens.findByToken(TokenHasher.sha256("missing"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("old", "New-Password-9")))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("missing", "New-Password-9")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldNotAcceptVerificationTokenForPasswordReset() {
        var verify = PasswordResetToken.builder().userId(user.getId()).token(TokenHasher.sha256("v"))
                .type("VERIFY_EMAIL").expiresAt(OffsetDateTime.now().plusHours(1)).build();
        when(passwordResetTokens.findByToken(TokenHasher.sha256("v"))).thenReturn(Optional.of(verify));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("v", "New-Password-9")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldVerifyEmail() {
        user.setEmailVerified(false);
        var token = PasswordResetToken.builder().userId(user.getId()).token(TokenHasher.sha256("v"))
                .type("VERIFY_EMAIL").expiresAt(OffsetDateTime.now().plusHours(1)).build();
        when(passwordResetTokens.findByToken(TokenHasher.sha256("v"))).thenReturn(Optional.of(token));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        authService.verifyEmail("v");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
    }
}
