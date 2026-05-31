package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateTreatmentPlanRequest;
import com.br.psyke.psyke.dto.TreatmentPlanResponse;
import com.br.psyke.psyke.dto.UpdateTreatmentPlanRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.TreatmentPlanService;
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
@RequestMapping("/treatment-plans")
@RequiredArgsConstructor
@Tag(name = "Treatment Plans", description = "Therapeutic treatment plan management within the current clinic")
public class TreatmentPlanController {

    private final TreatmentPlanService service;

    @GetMapping
    @Operation(summary = "List treatment plans with pagination")
    public ResponseEntity<Page<TreatmentPlanResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by prontuario ID") UUID prontuarioId,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), prontuarioId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get treatment plan by ID")
    public ResponseEntity<TreatmentPlanResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Create a new treatment plan")
    public ResponseEntity<TreatmentPlanResponse> create(@Valid @RequestBody CreateTreatmentPlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing treatment plan")
    public ResponseEntity<TreatmentPlanResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreatmentPlanRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a treatment plan")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
