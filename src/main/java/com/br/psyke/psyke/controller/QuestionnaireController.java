package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateQuestionnaireRequest;
import com.br.psyke.psyke.dto.QuestionnaireResponse;
import com.br.psyke.psyke.dto.UpdateQuestionnaireRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/questionnaires")
@RequiredArgsConstructor
@Tag(name = "Questionnaires", description = "Questionnaire template management")
public class QuestionnaireController {

    private final QuestionnaireService service;

    @GetMapping
    @Operation(summary = "List questionnaire templates with pagination")
    public ResponseEntity<Page<QuestionnaireResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(
                UUID.fromString(TenantContext.getClinicId()), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get questionnaire template by ID")
    public ResponseEntity<QuestionnaireResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @PostMapping
    @Operation(summary = "Create a questionnaire template")
    public ResponseEntity<QuestionnaireResponse> create(@Valid @RequestBody CreateQuestionnaireRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a questionnaire template")
    public ResponseEntity<QuestionnaireResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionnaireRequest req) {
        return ResponseEntity.ok(service.update(id, req,
                UUID.fromString(TenantContext.getClinicId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a questionnaire template")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, UUID.fromString(TenantContext.getClinicId()));
        return ResponseEntity.noContent().build();
    }
}
