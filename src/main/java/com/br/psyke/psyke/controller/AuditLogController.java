package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.AuditLogResponse;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit trail queries (read-only)")
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    @Operation(summary = "Query audit logs with pagination and optional filters")
    public ResponseEntity<Page<AuditLogResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by entity type (e.g. PATIENT, APPOINTMENT)") String entityType,
            @RequestParam(required = false) @Parameter(description = "Filter by entity ID") UUID entityId,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), entityType, entityId, pageable));
    }
}
