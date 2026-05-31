package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.ForgotPasswordRequest;
import com.br.psyke.psyke.dto.ResetPasswordRequest;
import com.br.psyke.psyke.exception.InvalidTokenException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.PasswordResetToken;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.repository.PasswordResetTokenRepository;
import com.br.psyke.psyke.repository.UserRepository;
import com.br.psyke.psyke.service.AuthService;
import com.br.psyke.psyke.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceNotificationTest {

    @Mock private UserRepository users;
    @Mock private PasswordResetTokenRepository passwordResetTokens;
    @Mock private EmailService emailService;
    private PasswordEncoder encoder;
    private AuthService authService;
    private UUID userId;
    private UUID tenantId;
    private User existingUser;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        authService = new AuthService(users, null, null, null, passwordResetTokens, null, encoder, null, emailService);
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        existingUser = User.builder().id(userId).tenantId(tenantId)
                .fullName("Test User").email("test@test.com")
                .passwordHash(encoder.encode("oldPass"))
                .active(true).emailVerified(false).build();
    }

    @Test
    void shouldSendForgotPasswordEmail() {
        var req = new ForgotPasswordRequest("test@test.com", tenantId.toString());
        var savedToken = PasswordResetToken.builder().id(UUID.randomUUID())
                .userId(userId).token("token-123").expiresAt(OffsetDateTime.now().plusHours(1))
                .type("PASSWORD_RESET").build();

        when(users.findByEmailAndTenantId(req.email(), tenantId)).thenReturn(Optional.of(existingUser));
        when(passwordResetTokens.save(any())).thenReturn(savedToken);

        authService.forgotPassword(req);

        verify(emailService).sendPasswordReset(eq("test@test.com"), eq("Test User"), anyString());
    }

    @Test
    void shouldThrowOnForgotPasswordWhenUserNotFound() {
        var req = new ForgotPasswordRequest("unknown@test.com", tenantId.toString());
        when(users.findByEmailAndTenantId(req.email(), tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldResetPassword() {
        var token = "valid-token";
        var resetToken = PasswordResetToken.builder().id(UUID.randomUUID())
                .userId(userId).token(token).expiresAt(OffsetDateTime.now().plusHours(1))
                .type("PASSWORD_RESET").build();
        var req = new ResetPasswordRequest(token, "newPassword123");

        when(passwordResetTokens.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(users.findById(userId)).thenReturn(Optional.of(existingUser));

        authService.resetPassword(req);

        assertThat(encoder.matches("newPassword123", existingUser.getPasswordHash())).isTrue();
        assertThat(resetToken.getUsedAt()).isNotNull();
    }

    @Test
    void shouldThrowOnResetPasswordWhenTokenExpired() {
        var token = "expired-token";
        var resetToken = PasswordResetToken.builder().id(UUID.randomUUID())
                .userId(userId).token(token).expiresAt(OffsetDateTime.now().minusHours(1))
                .type("PASSWORD_RESET").build();
        var req = new ResetPasswordRequest(token, "newPassword123");

        when(passwordResetTokens.findByToken(token)).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldThrowOnResetPasswordWhenTokenNotFound() {
        var req = new ResetPasswordRequest("invalid-token", "newPassword123");
        when(passwordResetTokens.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldVerifyEmail() {
        var token = "verify-token";
        var vt = PasswordResetToken.builder().id(UUID.randomUUID())
                .userId(userId).token(token).type("VERIFY_EMAIL")
                .expiresAt(OffsetDateTime.now().plusHours(48)).build();

        when(passwordResetTokens.findByToken(token)).thenReturn(Optional.of(vt));
        when(users.findById(userId)).thenReturn(Optional.of(existingUser));

        authService.verifyEmail(token);

        assertThat(existingUser.isEmailVerified()).isTrue();
        assertThat(vt.getUsedAt()).isNotNull();
    }

    @Test
    void shouldThrowOnVerifyEmailWhenWrongType() {
        var token = "wrong-type-token";
        var vt = PasswordResetToken.builder().id(UUID.randomUUID())
                .userId(userId).token(token).type("PASSWORD_RESET")
                .expiresAt(OffsetDateTime.now().plusHours(48)).build();

        when(passwordResetTokens.findByToken(token)).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("token type");
    }

    @Test
    void shouldSendEmailVerification() {
        when(users.findByIdWithRoles(userId)).thenReturn(Optional.of(existingUser));
        var saved = PasswordResetToken.builder().id(UUID.randomUUID())
                .userId(userId).token("vt-123").type("VERIFY_EMAIL")
                .expiresAt(OffsetDateTime.now().plusHours(48)).build();
        when(passwordResetTokens.save(any())).thenReturn(saved);

        authService.sendEmailVerification(userId);

        verify(emailService).sendEmailVerification(eq("test@test.com"), eq("Test User"), anyString());
    }
}
