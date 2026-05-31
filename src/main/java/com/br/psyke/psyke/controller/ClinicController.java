package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.ClinicResponse;
import com.br.psyke.psyke.dto.CreateClinicRequest;
import com.br.psyke.psyke.dto.UpdateClinicRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.ClinicService;
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
@RequestMapping("/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinic management within the current tenant")
public class ClinicController {

    private final ClinicService service;

    @GetMapping
    @Operation(summary = "List clinics with pagination")
    public ResponseEntity<Page<ClinicResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current clinic from JWT context")
    public ResponseEntity<ClinicResponse> findCurrent() {
        return ResponseEntity.ok(service.findById(
                UUID.fromString(TenantContext.getClinicId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinic by ID")
    public ResponseEntity<ClinicResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new clinic (admin only)")
    public ResponseEntity<ClinicResponse> create(@Valid @RequestBody CreateClinicRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing clinic (admin only)")
    public ResponseEntity<ClinicResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a clinic (admin only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
