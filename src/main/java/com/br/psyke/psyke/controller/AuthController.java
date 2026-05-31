package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService svc;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest r, HttpServletRequest h) {
        return ResponseEntity.ok(svc.login(r, h.getRemoteAddr(), h.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest r, HttpServletRequest h) {
        return ResponseEntity.ok(svc.refresh(r, h.getRemoteAddr(), h.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest r) {
        svc.logout(r.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication a) {
        return ResponseEntity.ok(svc.me((UUID) a.getPrincipal()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest r) {
        svc.forgotPassword(r);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest r) {
        svc.resetPassword(r);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest r) {
        svc.verifyEmail(r.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-verification")
    public ResponseEntity<Void> sendVerification(Authentication a) {
        svc.sendEmailVerification((UUID) a.getPrincipal());
        return ResponseEntity.ok().build();
    }
}
