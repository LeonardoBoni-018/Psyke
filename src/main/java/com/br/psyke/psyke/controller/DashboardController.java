package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.DashboardResponse;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Clinic dashboard and aggregated reports")
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    @Operation(summary = "Get dashboard summary", description = "Returns aggregated stats: patient/professional counts, today's appointments, monthly revenue")
    public ResponseEntity<DashboardResponse> getSummary() {
        return ResponseEntity.ok(service.getSummary(
                UUID.fromString(TenantContext.getClinicId())));
    }
}
