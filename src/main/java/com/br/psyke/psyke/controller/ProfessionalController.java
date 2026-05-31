package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateProfessionalRequest;
import com.br.psyke.psyke.dto.ProfessionalResponse;
import com.br.psyke.psyke.dto.UpdateProfessionalRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
@Tag(name = "Professionals", description = "Professional profile management within the current clinic")
public class ProfessionalController {

    private final ProfessionalService service;

    @GetMapping
    @Operation(summary = "List professionals with pagination")
    public ResponseEntity<Page<ProfessionalResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get professional by ID")
    public ResponseEntity<ProfessionalResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Create a new professional")
    public ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody CreateProfessionalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing professional")
    public ResponseEntity<ProfessionalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfessionalRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a professional (admin only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
