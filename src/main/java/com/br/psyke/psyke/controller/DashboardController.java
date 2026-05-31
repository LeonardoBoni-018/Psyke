package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Summary stats and today's sessions for the dashboard")
public class DashboardController {

    private final AppointmentRepository appointmentRepo;

    @GetMapping("/summary")
    @Operation(summary = "Dashboard statistics", description = "Returns today's session count, pending confirmations summary")
    public ResponseEntity<Map<String, Object>> summary() {
        var today = LocalDate.now();
        var startOfDay = today.atStartOfDay();
        var endOfDay = today.atTime(23, 59, 59);

        var sessionsToday = appointmentRepo.findAllByFilters(null, null, null, startOfDay, endOfDay).size();
        var pending = appointmentRepo.findAllByFilters(null, null,
                Appointment.AppointmentStatus.SCHEDULED, startOfDay, endOfDay).size();

        return ResponseEntity.ok(Map.of(
            "sessionsToday", sessionsToday,
            "pendingConfirmation", pending,
            "date", today.toString()
        ));
    }

    @GetMapping("/today-sessions")
    @Operation(summary = "Today's appointments", description = "Returns all appointments scheduled for today ordered by start time")
    public ResponseEntity<List<Map<String, Object>>> todaySessions() {
        var start = LocalDateTime.now().toLocalDate().atStartOfDay();
        var end = start.plusDays(1).minusSeconds(1);
        var sessions = appointmentRepo.findAllByFilters(null, null, null, start, end);
        var result = sessions.stream().map(a -> Map.<String, Object>of(
            "id",            a.getId(),
            "patientId",     a.getPatientId(),
            "professionalId", a.getProfessionalId(),
            "startTime",     a.getStartTime().toString(),
            "endTime",       a.getEndTime().toString(),
            "status",        a.getStatus().name(),
            "notes",         a.getNotes() != null ? a.getNotes() : ""
        )).toList();
        return ResponseEntity.ok(result);
    }
}
