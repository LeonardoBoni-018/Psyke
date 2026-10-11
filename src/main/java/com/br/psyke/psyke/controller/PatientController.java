package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreatePatientRequest;
import com.br.psyke.psyke.dto.PatientResponse;
import com.br.psyke.psyke.dto.UpdatePatientRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.PatientService;
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
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient profile management within the current clinic")
public class PatientController {

    private final PatientService service;

    @GetMapping
    @Operation(summary = "List patients with pagination")
    public ResponseEntity<Page<PatientResponse>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        if (q != null && q.length() > 100) throw new IllegalArgumentException("Busca muito longa");
        return ResponseEntity.ok(service.search(UUID.fromString(TenantContext.getClinicId()), q, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<PatientResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Create a new patient")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody CreatePatientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing patient")
    public ResponseEntity<PatientResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a patient (admin only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
