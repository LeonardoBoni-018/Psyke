package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.service.PdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/export/pdf")
@RequiredArgsConstructor
@Tag(name = "PDF Export", description = "Export entities as PDF documents")
public class PdfExportController {

    private final PdfExportService pdfService;

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Export appointment as PDF")
    public ResponseEntity<byte[]> appointment(@PathVariable UUID id) {
        var pdf = pdfService.exportAppointment(id);
        return ok(pdf, "appointment-" + id + ".pdf");
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Export invoice as PDF")
    public ResponseEntity<byte[]> invoice(@PathVariable UUID id) {
        var pdf = pdfService.exportInvoice(id);
        return ok(pdf, "invoice-" + id + ".pdf");
    }

    @GetMapping("/prescriptions/{id}")
    @Operation(summary = "Export prescription as PDF")
    public ResponseEntity<byte[]> prescription(@PathVariable UUID id) {
        var pdf = pdfService.exportPrescription(id);
        return ok(pdf, "prescription-" + id + ".pdf");
    }

    @GetMapping("/consent-terms/{id}")
    @Operation(summary = "Export consent term as PDF")
    public ResponseEntity<byte[]> consentTerm(@PathVariable UUID id) {
        var pdf = pdfService.exportConsentTerm(id);
        return ok(pdf, "consent-term-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> ok(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
