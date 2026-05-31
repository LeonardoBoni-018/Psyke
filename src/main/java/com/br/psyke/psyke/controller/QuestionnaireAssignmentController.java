package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.QuestionnaireAssignmentResponse;
import com.br.psyke.psyke.dto.SubmitAnswersRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.QuestionnaireAssignmentService;
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
@RequestMapping("/questionnaire-assignments")
@RequiredArgsConstructor
@Tag(name = "Questionnaire Assignments", description = "Assign and submit questionnaires to patients")
public class QuestionnaireAssignmentController {

    private final QuestionnaireAssignmentService service;

    @GetMapping
    @Operation(summary = "List assignments with pagination")
    public ResponseEntity<Page<QuestionnaireAssignmentResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") UUID patientId,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assignment by ID")
    public ResponseEntity<QuestionnaireAssignmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Assign a questionnaire to a patient")
    public ResponseEntity<QuestionnaireAssignmentResponse> assign(
            @RequestParam @Parameter(description = "Questionnaire template ID") UUID questionnaireId,
            @RequestParam @Parameter(description = "Patient ID") UUID patientId,
            @RequestParam(required = false) @Parameter(description = "Professional ID") UUID professionalId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.assign(UUID.fromString(TenantContext.getClinicId()),
                        questionnaireId, patientId, professionalId));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit answers for an assignment")
    public ResponseEntity<QuestionnaireAssignmentResponse> submit(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAnswersRequest req) {
        return ResponseEntity.ok(service.submit(id,
                UUID.fromString(TenantContext.getClinicId()), req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an assignment")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
