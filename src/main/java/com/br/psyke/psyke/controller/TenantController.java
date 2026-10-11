package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final AuthService svc;

    /** Public clinic signup (rate limited). */
    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.createTenant(r));
    }

    /** The caller's own tenant. Tenants are never readable by id, so ids cannot be enumerated. */
    @GetMapping("/current")
    public ResponseEntity<TenantResponse> current() {
        return ResponseEntity.ok(svc.getTenant(UUID.fromString(TenantContext.getTenantId())));
    }
}
