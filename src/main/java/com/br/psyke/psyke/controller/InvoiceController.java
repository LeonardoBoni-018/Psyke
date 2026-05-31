package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateInvoiceRequest;
import com.br.psyke.psyke.dto.InvoiceResponse;
import com.br.psyke.psyke.dto.UpdateInvoiceRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Session billing and invoice management within the current clinic")
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping
    @Operation(summary = "List invoices with pagination and optional filters")
    public ResponseEntity<Page<InvoiceResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") UUID patientId,
            @RequestParam(required = false) @Parameter(description = "Filter by status (PENDING, PAID, CANCELLED, REFUNDED)") String status,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new invoice (admin only)")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing invoice (admin only)")
    public ResponseEntity<InvoiceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an invoice (admin only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
