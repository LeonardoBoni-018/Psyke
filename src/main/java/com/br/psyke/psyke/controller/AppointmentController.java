package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.AppointmentRecurrenceRequest;
import com.br.psyke.psyke.dto.AppointmentRequest;
import com.br.psyke.psyke.dto.AppointmentResponse;
import com.br.psyke.psyke.dto.CalendarEvent;
import com.br.psyke.psyke.dto.CreateSessionRequest;
import com.br.psyke.psyke.dto.StatusUpdateRequest;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.service.AppointmentService;
import com.br.psyke.psyke.service.RecurrenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment scheduling, status management, calendar, and session creation")
public class AppointmentController {

    private final AppointmentService service;
    private final RecurrenceService recurrenceService;

    @PostMapping
    @Operation(summary = "Schedule a new appointment", description = "Creates an appointment with conflict validation (professional, room, slot)")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    @Operation(summary = "List appointments with filters", description = "Returns filtered appointments. All parameters are optional.")
    public ResponseEntity<List<AppointmentResponse>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by professional ID") UUID professionalId,
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") UUID patientId,
            @RequestParam(required = false) @Parameter(description = "Filter by status (SCHEDULED, CONFIRMED, CANCELLED, NO_SHOW, DONE)") Appointment.AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Filter appointments starting from this date") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Filter appointments ending before this date") LocalDateTime to) {
        return ResponseEntity.ok(service.findAll(professionalId, patientId, status, from, to));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get calendar events", description = "Returns appointments as FullCalendar-compatible event resources")
    public ResponseEntity<List<CalendarEvent>> calendar(
            @RequestParam(required = false) @Parameter(description = "Filter by professional ID") UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Start of date range") LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "End of date range") LocalDateTime to) {
        return ResponseEntity.ok(service.findCalendarEvents(professionalId, from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition appointment status", description = "Validates state machine: SCHEDULED→CONFIRMED|CANCELLED|NO_SHOW, CONFIRMED→CANCELLED|NO_SHOW|DONE")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest req) {
        return ResponseEntity.ok(service.updateStatus(id, req.status(), req.reason()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a SCHEDULED appointment", description = "Removes the appointment and frees the associated slots. Only SCHEDULED appointments can be deleted.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/session")
    @Operation(summary = "Create session from confirmed appointment", description = "Marks a CONFIRMED appointment as DONE and publishes a SessionEvent for psyke-prontuario")
    public ResponseEntity<AppointmentResponse> createSession(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CreateSessionRequest req) {
        var prontuario = req != null ? req.prontuario() : null;
        return ResponseEntity.ok(service.createSession(id, prontuario));
    }

    @PostMapping("/{id}/recurrence")
    @Operation(summary = "Add recurring schedule to an existing appointment", description = "Generates future occurrences based on the recurrence rule")
    public ResponseEntity<List<AppointmentResponse>> addRecurrence(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentRecurrenceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurrenceService.createRecurrenceForAppointment(id, req.freq(), req.interval(), req.count(), req.until()));
    }
}
