package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.DocumentResponse;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document and attachment management with file upload/download")
public class DocumentController {

    private final DocumentService service;

    @GetMapping
    @Operation(summary = "List documents with pagination")
    public ResponseEntity<Page<DocumentResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") UUID patientId,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata by ID")
    public ResponseEntity<DocumentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document file")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        var clinicId = UUID.fromString(TenantContext.getClinicId());
        var resource = service.download(id, clinicId);
        var filename = service.getOriginalFilename(id, clinicId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a document")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) @Parameter(description = "Patient ID") UUID patientId,
            @RequestParam(required = false) @Parameter(description = "Appointment ID") UUID appointmentId,
            @RequestParam(required = false) @Parameter(description = "Professional ID") UUID professionalId,
            @RequestParam(required = false) @Parameter(description = "Document notes") String notes) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.upload(file, UUID.fromString(TenantContext.getClinicId()),
                        patientId, appointmentId, professionalId, notes));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and its file")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
