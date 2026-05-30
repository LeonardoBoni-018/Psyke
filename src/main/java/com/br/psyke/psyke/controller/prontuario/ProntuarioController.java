package com.br.psyke.psyke.controller.prontuario;

import com.br.psyke.psyke.dto.prontuario.*;
import com.br.psyke.psyke.service.prontuario.ProntuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/prontuarios")
@RequiredArgsConstructor
@Tag(name = "Prontuário Eletrônico", description = "Electronic medical records: prontuários, clinical evolutions, and anamnesis")
public class ProntuarioController {

    private final ProntuarioService service;

    @PostMapping
    @Operation(summary = "Create a new prontuário for a patient")
    public ResponseEntity<ProntuarioResponse> create(@Valid @RequestBody ProntuarioRequest req, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, userId, isAdmin));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find prontuário by ID")
    public ResponseEntity<ProntuarioResponse> findById(@PathVariable UUID id, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var roles = extractRoles(auth);
        return ResponseEntity.ok(service.findById(id, userId, roles.isAdmin, roles.isPatient, roles.isReceptionist));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Find prontuário by patient ID")
    public ResponseEntity<ProntuarioResponse> findByPatient(@PathVariable UUID patientId, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var roles = extractRoles(auth);
        return ResponseEntity.ok(service.findByPatient(patientId, userId, roles.isAdmin, roles.isPatient, roles.isReceptionist));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update prontuário fields")
    public ResponseEntity<ProntuarioResponse> update(@PathVariable UUID id, @Valid @RequestBody ProntuarioRequest req, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(service.update(id, req, userId, isAdmin));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a prontuário (admin only)")
    public ResponseEntity<ProntuarioResponse> archive(@PathVariable UUID id, Authentication auth) {
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(service.archive(id, isAdmin));
    }

    @PostMapping("/{id}/evolucoes")
    @Operation(summary = "Add clinical evolution to a prontuário")
    public ResponseEntity<EvolucaoClinicaResponse> addEvolucao(
            @PathVariable UUID id, @Valid @RequestBody EvolucaoClinicaRequest req, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addEvolucao(id, req, userId, isAdmin));
    }

    @GetMapping("/{id}/evolucoes")
    @Operation(summary = "List clinical evolutions for a prontuário")
    public ResponseEntity<List<EvolucaoClinicaResponse>> listEvolucoes(@PathVariable UUID id, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var roles = extractRoles(auth);
        return ResponseEntity.ok(service.listEvolucoes(id, userId, roles.isAdmin, roles.isPatient, roles.isReceptionist));
    }

    @PostMapping("/{id}/anamnese")
    @Operation(summary = "Create anamnesis for a prontuário")
    public ResponseEntity<AnamneseResponse> createAnamnese(
            @PathVariable UUID id, @Valid @RequestBody AnamneseRequest req, Authentication auth) {
        if (!req.prontuarioId().equals(id))
            throw new IllegalArgumentException("Path ID must match prontuarioId in body");
        var userId = (UUID) auth.getPrincipal();
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAnamnese(req, userId, isAdmin));
    }

    @GetMapping("/{id}/anamnese")
    @Operation(summary = "Get anamnesis for a prontuário")
    public ResponseEntity<AnamneseResponse> findAnamnese(@PathVariable UUID id, Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var roles = extractRoles(auth);
        return ResponseEntity.ok(service.findAnamnese(id, userId, roles.isAdmin, roles.isPatient, roles.isReceptionist));
    }

    @PutMapping("/{id}/anamnese")
    @Operation(summary = "Update anamnesis for a prontuário")
    public ResponseEntity<AnamneseResponse> updateAnamnese(
            @PathVariable UUID id, @Valid @RequestBody AnamneseRequest req, Authentication auth) {
        if (!req.prontuarioId().equals(id))
            throw new IllegalArgumentException("Path ID must match prontuarioId in body");
        var userId = (UUID) auth.getPrincipal();
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(service.updateAnamnese(id, req, userId, isAdmin));
    }

    private Roles extractRoles(Authentication auth) {
        var isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        var isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        var isReceptionist = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RECEPTIONIST"));
        return new Roles(isAdmin, isPatient, isReceptionist);
    }

    private record Roles(boolean isAdmin, boolean isPatient, boolean isReceptionist) {}
}
