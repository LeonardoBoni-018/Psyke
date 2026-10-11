package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateUserRequest;
import com.br.psyke.psyke.dto.UpdateRolesRequest;
import com.br.psyke.psyke.dto.UpdateUserRequest;
import com.br.psyke.psyke.dto.UserResponse;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management within the authenticated tenant")
public class UserController {

    private final UserService service;

    @GetMapping
    @Operation(summary = "List users in the current tenant with pagination")
    public ResponseEntity<Page<UserResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(tenantId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id, tenantId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing user (admin)")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req,
                                               Authentication auth) {
        return ResponseEntity.ok(service.update(id, req, tenantId(), (UUID) auth.getPrincipal()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user (admin)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        service.delete(id, tenantId(), (UUID) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user in the current tenant (admin)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, tenantId()));
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replace the roles of a user (admin)")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateRolesRequest req,
                                                    Authentication auth) {
        return ResponseEntity.ok(service.updateRoles(id, req.roles(), tenantId(), (UUID) auth.getPrincipal()));
    }

    private static UUID tenantId() {
        return UUID.fromString(TenantContext.getTenantId());
    }
}
