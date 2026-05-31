package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.ConsentTermResponse;
import com.br.psyke.psyke.dto.CreateConsentTermRequest;
import com.br.psyke.psyke.dto.UpdateConsentTermRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.ConsentTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/consent-terms")
@RequiredArgsConstructor
@Tag(name = "Consent Terms", description = "Digital informed consent management")
public class ConsentTermController {

    private final ConsentTermService service;

    @GetMapping
    @Operation(summary = "List consent terms with pagination")
    public ResponseEntity<Page<ConsentTermResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") UUID patientId,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consent term by ID")
    public ResponseEntity<ConsentTermResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Create a new consent term")
    public ResponseEntity<ConsentTermResponse> create(@Valid @RequestBody CreateConsentTermRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "Sign a consent term", description = "Sets signedByProfessional=true if asProfessional=true, otherwise signedByPatient=true")
    public ResponseEntity<ConsentTermResponse> sign(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") @Parameter(description = "Sign as professional (vs patient)") boolean asProfessional) {
        return ResponseEntity.ok(service.sign(id,
                UUID.fromString(TenantContext.getClinicId()), asProfessional));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a consent term")
    public ResponseEntity<ConsentTermResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConsentTermRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a consent term")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
