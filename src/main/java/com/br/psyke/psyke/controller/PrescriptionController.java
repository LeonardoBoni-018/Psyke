package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreatePrescriptionRequest;
import com.br.psyke.psyke.dto.PrescriptionResponse;
import com.br.psyke.psyke.dto.UpdatePrescriptionRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.PrescriptionService;
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
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Medical prescription management within the current clinic")
public class PrescriptionController {

    private final PrescriptionService service;

    @GetMapping
    @Operation(summary = "List prescriptions with pagination")
    public ResponseEntity<Page<PrescriptionResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") UUID patientId,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID")
    public ResponseEntity<PrescriptionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Create a new prescription")
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing prescription")
    public ResponseEntity<PrescriptionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePrescriptionRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
