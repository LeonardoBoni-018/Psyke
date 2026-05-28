package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.service.ProfessionalAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/professionals/{professionalId}")
@RequiredArgsConstructor
public class ProfessionalAvailabilityController {

    private final ProfessionalAvailabilityService service;

    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer duration) {
        return ResponseEntity.ok(service.getSlots(professionalId, from, to, duration));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> listAvailability(@PathVariable UUID professionalId) {
        return ResponseEntity.ok(service.listAvailability(professionalId));
    }

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @PathVariable UUID professionalId,
            @Valid @RequestBody AvailabilityRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAvailability(professionalId, req));
    }

    @PutMapping("/availability/{id}")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @PathVariable UUID professionalId,
            @PathVariable UUID id,
            @Valid @RequestBody AvailabilityRequest req) {
        return ResponseEntity.ok(service.updateAvailability(id, req));
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable UUID id) {
        service.deleteAvailability(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exceptions")
    public ResponseEntity<List<ExceptionResponse>> listExceptions(@PathVariable UUID professionalId) {
        return ResponseEntity.ok(service.listExceptions(professionalId));
    }

    @PostMapping("/exceptions")
    public ResponseEntity<ExceptionResponse> createException(
            @PathVariable UUID professionalId,
            @Valid @RequestBody ExceptionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createException(professionalId, req));
    }

    @DeleteMapping("/exceptions/{id}")
    public ResponseEntity<Void> deleteException(@PathVariable UUID id) {
        service.deleteException(id);
        return ResponseEntity.noContent().build();
    }
}
