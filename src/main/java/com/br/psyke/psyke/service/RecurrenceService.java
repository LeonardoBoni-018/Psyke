package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.AppointmentResponse;
import com.br.psyke.psyke.dto.RecurrenceRequest;
import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurrenceService {

    private static final int MAX_WEEKS = 52;

    private final AppointmentRepository appointmentRepo;
    private final RecurrenceRuleRepository ruleRepo;
    private final SlotRepository slotRepo;
    private final UserRepository userRepo;
    private final RoomRepository roomRepo;
    private final AppointmentEventProducer eventProducer;

    @Transactional
    public List<AppointmentResponse> createRecurring(RecurrenceRequest req) {
        var professional = userRepo.findById(req.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
        userRepo.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (req.roomId() != null) {
            roomRepo.findById(req.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        }

        var duration = ChronoUnit.MINUTES.between(req.startTime(), req.endTime());
        if (duration <= 0) throw new IllegalArgumentException("End time must be after start time");

        var occurrences = generateOccurrences(req);
        var responses = new ArrayList<AppointmentResponse>();

        for (var start : occurrences) {
            var end = start.plusMinutes(duration);

            var slotFree = slotRepo.existsByProfessionalIdAndStartTimeAndStatus(
                    req.professionalId(), start, Slot.SlotStatus.FREE);
            if (!slotFree) continue;

            var profConflicts = appointmentRepo.findConflictingByProfessional(
                    req.professionalId(), start, end);
            if (!profConflicts.isEmpty()) continue;

            if (req.roomId() != null) {
                var roomConflicts = appointmentRepo.findConflictingByRoom(
                        req.roomId(), start, end);
                if (!roomConflicts.isEmpty()) continue;
            }

            var appointment = Appointment.builder()
                    .clinicId(professional.getClinicId())
                    .patientId(req.patientId())
                    .professionalId(req.professionalId())
                    .roomId(req.roomId())
                    .startTime(start)
                    .endTime(end)
                    .status(Appointment.AppointmentStatus.SCHEDULED)
                    .notes(req.notes())
                    .build();
            appointment = appointmentRepo.save(appointment);

            var slots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                    req.professionalId(), start, end);
            for (var s : slots) {
                s.setStatus(Slot.SlotStatus.BOOKED);
                s.setPatientId(req.patientId());
                s.setRoomId(req.roomId());
            }
            slotRepo.saveAll(slots);

            eventProducer.publish(new AppointmentEvent("scheduled", appointment.getId(),
                    appointment.getClinicId(), appointment.getPatientId(),
                    appointment.getProfessionalId(), appointment.getRoomId(),
                    start.toString(), end.toString(),
                    appointment.getStatus().name(), LocalDateTime.now()));

            if (responses.isEmpty()) {
                var freqStr = req.freq() != null ? req.freq() : "WEEKLY";
                var intervalVal = req.interval() != null ? req.interval() : 1;
                ruleRepo.save(RecurrenceRule.builder()
                        .appointmentId(appointment.getId())
                        .freq(freqStr)
                        .interval(intervalVal)
                        .count(occurrences.size())
                        .until(req.until())
                        .build());
            }

            responses.add(toResponse(appointment));
        }

        if (responses.isEmpty()) throw new SlotConflictException("No available slots for the recurring schedule");
        return responses;
    }

    private List<LocalDateTime> generateOccurrences(RecurrenceRequest req) {
        var results = new ArrayList<LocalDateTime>();
        var freq = req.freq() != null ? req.freq() : "WEEKLY";
        var interval = req.interval() != null ? req.interval() : 1;
        var count = req.count() != null ? req.count() : Integer.MAX_VALUE;
        var until = req.until() != null ? req.until().atStartOfDay() : req.startTime().plusWeeks(MAX_WEEKS);
        var maxTime = req.startTime().plusWeeks(MAX_WEEKS);
        var effectiveUntil = until.isAfter(maxTime) ? maxTime : until;

        var current = req.startTime();
        var generated = 0;

        while (generated < count && !current.isAfter(effectiveUntil)) {
            results.add(current);
            generated++;
            if ("WEEKLY".equalsIgnoreCase(freq)) {
                current = current.plusWeeks(interval);
            } else {
                current = current.plusWeeks(interval);
            }
        }

        return results;
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getClinicId(), a.getPatientId(),
                a.getProfessionalId(), a.getRoomId(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getStatus().name(), a.getProntuario(), a.getNotes(),
                a.getCreatedAt().toString(), a.getUpdatedAt().toString());
    }
}
