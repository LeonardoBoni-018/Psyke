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
import java.time.LocalDate;
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
    private final SlotEngine slotEngine;

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

            var date = start.toLocalDate();
            var freeSlots = slotEngine.generateFreeSlots(req.professionalId(), date, date, null);
            if (freeSlots.stream().noneMatch(dt -> dt.equals(start)))
                continue;

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

            var existingSlots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                    req.professionalId(), start, end);
            if (existingSlots.isEmpty()) {
                slotRepo.save(Slot.builder()
                        .professionalId(req.professionalId())
                        .startTime(start)
                        .endTime(end)
                        .status(Slot.SlotStatus.BOOKED)
                        .patientId(req.patientId())
                        .roomId(req.roomId())
                        .build());
            } else {
                for (var s : existingSlots) {
                    s.setStatus(Slot.SlotStatus.BOOKED);
                    s.setPatientId(req.patientId());
                    s.setRoomId(req.roomId());
                }
                slotRepo.saveAll(existingSlots);
            }

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

    @Transactional
    public List<AppointmentResponse> createRecurrenceForAppointment(UUID appointmentId, String freq, Integer interval, Integer count, LocalDate until) {
        var source = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        var start = source.getStartTime();
        var end = source.getEndTime();
        var duration = ChronoUnit.MINUTES.between(start, end);
        if (duration <= 0) throw new IllegalArgumentException("Invalid appointment duration");

        var occurrences = generateOccurrences(start, duration, freq, interval, count, until);
        var responses = new ArrayList<AppointmentResponse>();
        var ruleSaved = false;

        for (var occStart : occurrences) {
            if (occStart.equals(start)) continue;
            var occEnd = occStart.plusMinutes(duration);

            var date = occStart.toLocalDate();
            var freeSlots = slotEngine.generateFreeSlots(source.getProfessionalId(), date, date, null);
            if (freeSlots.stream().noneMatch(dt -> dt.equals(occStart)))
                continue;

            var profConflicts = appointmentRepo.findConflictingByProfessional(
                    source.getProfessionalId(), occStart, occEnd);
            if (!profConflicts.isEmpty()) continue;

            if (source.getRoomId() != null) {
                var roomConflicts = appointmentRepo.findConflictingByRoom(
                        source.getRoomId(), occStart, occEnd);
                if (!roomConflicts.isEmpty()) continue;
            }

            var appointment = Appointment.builder()
                    .clinicId(source.getClinicId())
                    .patientId(source.getPatientId())
                    .professionalId(source.getProfessionalId())
                    .roomId(source.getRoomId())
                    .startTime(occStart)
                    .endTime(occEnd)
                    .status(Appointment.AppointmentStatus.SCHEDULED)
                    .notes(source.getNotes())
                    .build();
            appointment = appointmentRepo.save(appointment);

            var existingSlots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                    source.getProfessionalId(), occStart, occEnd);
            if (existingSlots.isEmpty()) {
                slotRepo.save(Slot.builder()
                        .professionalId(source.getProfessionalId())
                        .startTime(occStart)
                        .endTime(occEnd)
                        .status(Slot.SlotStatus.BOOKED)
                        .patientId(source.getPatientId())
                        .roomId(source.getRoomId())
                        .build());
            } else {
                for (var s : existingSlots) {
                    s.setStatus(Slot.SlotStatus.BOOKED);
                    s.setPatientId(source.getPatientId());
                    s.setRoomId(source.getRoomId());
                }
                slotRepo.saveAll(existingSlots);
            }

            eventProducer.publish(new AppointmentEvent("scheduled", appointment.getId(),
                    appointment.getClinicId(), appointment.getPatientId(),
                    appointment.getProfessionalId(), appointment.getRoomId(),
                    occStart.toString(), occEnd.toString(),
                    appointment.getStatus().name(), LocalDateTime.now()));

            if (!ruleSaved) {
                ruleRepo.save(RecurrenceRule.builder()
                        .appointmentId(source.getId())
                        .freq(freq != null ? freq : "WEEKLY")
                        .interval(interval != null ? interval : 1)
                        .count(count != null ? count : occurrences.size())
                        .until(until)
                        .build());
                ruleSaved = true;
            }

            responses.add(toResponse(appointment));
        }

        if (responses.isEmpty() && !ruleSaved)
            throw new SlotConflictException("No available slots for the recurring schedule");
        return responses;
    }

    private List<LocalDateTime> generateOccurrences(LocalDateTime startTime, long durationMinutes, String freq, Integer interval, Integer count, LocalDate until) {
        var results = new ArrayList<LocalDateTime>();
        var f = freq != null ? freq.toUpperCase() : "WEEKLY";
        var inv = interval != null ? interval : 1;
        var cnt = count != null ? count : Integer.MAX_VALUE;

        LocalDateTime maxTime;
        LocalDateTime effectiveUntil;
        if (until != null) {
            effectiveUntil = until.atStartOfDay();
            maxTime = startTime.plusWeeks(MAX_WEEKS);
        } else {
            effectiveUntil = startTime.plusWeeks(MAX_WEEKS);
            maxTime = effectiveUntil;
        }
        if (effectiveUntil.isAfter(maxTime)) effectiveUntil = maxTime;

        var current = startTime;
        var generated = 0;

        while (generated < cnt && !current.isAfter(effectiveUntil)) {
            results.add(current);
            generated++;
            current = switch (f) {
                case "DAILY" -> current.plusDays(inv);
                case "WEEKLY" -> current.plusWeeks(inv);
                case "MONTHLY" -> current.plusMonths(inv);
                case "YEARLY" -> current.plusYears(inv);
                default -> current.plusWeeks(inv);
            };
        }

        return results;
    }

    private List<LocalDateTime> generateOccurrences(RecurrenceRequest req) {
        var duration = ChronoUnit.MINUTES.between(req.startTime(), req.endTime());
        return generateOccurrences(req.startTime(), duration, req.freq(), req.interval(), req.count(), req.until());
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getClinicId(), a.getPatientId(),
                a.getProfessionalId(), a.getRoomId(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getStatus().name(), a.getProntuario(), a.getNotes(),
                a.getCreatedAt().toString(), a.getUpdatedAt().toString());
    }
}
