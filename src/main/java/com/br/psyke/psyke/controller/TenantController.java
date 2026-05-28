package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final AuthService svc;

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.createTenant(r));
    }
}
