package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.ReportSummaryResponse;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PROFESSIONAL')")
@Tag(name = "Reports", description = "Indicators and CSV exports")
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService service;

    @GetMapping("/summary")
    @Operation(summary = "Financial and clinical indicators for a period (max 366 days)")
    public ResponseEntity<ReportSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date") LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date (inclusive)") LocalDate to) {
        return ResponseEntity.ok(service.summary(clinicId(), from, to));
    }

    @GetMapping("/appointments")
    @Operation(summary = "Export appointments as CSV")
    public ResponseEntity<String> exportAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date") LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date") LocalDate to) {
        return csv("agendamentos_" + from + "_" + to + ".csv", service.exportAppointmentsCsv(clinicId(), from, to));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Export invoices as CSV")
    public ResponseEntity<String> exportInvoices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date") LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date") LocalDate to) {
        return csv("faturas_" + from + "_" + to + ".csv", service.exportInvoicesCsv(clinicId(), from, to));
    }

    private static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(CSV)
                .body(body);
    }

    private static UUID clinicId() {
        return UUID.fromString(TenantContext.getClinicId());
    }
}
