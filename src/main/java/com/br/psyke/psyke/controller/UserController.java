package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateUserRequest;
import com.br.psyke.psyke.dto.UpdateRolesRequest;
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
        return ResponseEntity.ok(service.findAll(UUID.fromString(TenantContext.getTenantId()), pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new user in the current tenant")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getTenantId())));
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Update roles for a user", description = "Replaces all existing roles with the given list")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolesRequest req) {
        return ResponseEntity.ok(service.updateRoles(id, req.roles()));
    }
}
