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
}
