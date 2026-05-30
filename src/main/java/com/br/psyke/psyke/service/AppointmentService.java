package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.AppointmentRequest;
import com.br.psyke.psyke.dto.AppointmentResponse;
import com.br.psyke.psyke.dto.CalendarEvent;
import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.event.SessionEvent;
import com.br.psyke.psyke.event.SessionEventProducer;
import com.br.psyke.psyke.exception.InvalidTransitionException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Slot;
import com.br.psyke.psyke.repository.AppointmentRepository;
import com.br.psyke.psyke.repository.RoomRepository;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Map<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> TRANSITIONS = Map.of(
        Appointment.AppointmentStatus.SCHEDULED, Set.of(Appointment.AppointmentStatus.CONFIRMED, Appointment.AppointmentStatus.CANCELLED, Appointment.AppointmentStatus.NO_SHOW),
        Appointment.AppointmentStatus.CONFIRMED, Set.of(Appointment.AppointmentStatus.CANCELLED, Appointment.AppointmentStatus.NO_SHOW, Appointment.AppointmentStatus.DONE),
        Appointment.AppointmentStatus.CANCELLED, Set.of(),
        Appointment.AppointmentStatus.NO_SHOW, Set.of(),
        Appointment.AppointmentStatus.DONE, Set.of()
    );

    private static final Map<Appointment.AppointmentStatus, String> CALENDAR_COLORS = Map.of(
        Appointment.AppointmentStatus.SCHEDULED, "#3788d8",
        Appointment.AppointmentStatus.CONFIRMED, "#28a745",
        Appointment.AppointmentStatus.CANCELLED, "#6c757d",
        Appointment.AppointmentStatus.NO_SHOW, "#fd7e14",
        Appointment.AppointmentStatus.DONE, "#20c997"
    );

    private final AppointmentRepository appointmentRepo;
    private final SlotRepository slotRepo;
    private final UserRepository userRepo;
    private final RoomRepository roomRepo;
    private final AppointmentEventProducer eventProducer;
    private final SessionEventProducer sessionEventProducer;
    private final CancellationLogService cancellationLogService;
    private final SlotEngine slotEngine;

    public List<AppointmentResponse> findAll(UUID professionalId, UUID patientId, Appointment.AppointmentStatus status, LocalDateTime from, LocalDateTime to) {
        return appointmentRepo.findAllByFilters(professionalId, patientId, status, from, to)
                .stream().map(this::toResponse).toList();
    }

    public List<CalendarEvent> findCalendarEvents(UUID professionalId, LocalDateTime from, LocalDateTime to) {
        var appointments = professionalId != null
            ? appointmentRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(professionalId, from, to)
            : appointmentRepo.findAllByFilters(null, null, null, from, to);
        return appointments.stream().map(a -> {
            var color = CALENDAR_COLORS.getOrDefault(a.getStatus(), "#3788d8");
            return new CalendarEvent(a.getId(), a.getNotes() != null ? a.getNotes() : "",
                a.getStartTime().toString(), a.getEndTime().toString(),
                color, color, "#ffffff",
                Map.of("status", a.getStatus().name(), "patientId", a.getPatientId().toString(),
                       "professionalId", a.getProfessionalId().toString(),
                       "roomId", a.getRoomId() != null ? a.getRoomId().toString() : ""));
        }).toList();
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest req) {
        var professional = userRepo.findById(req.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
        userRepo.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        var date = req.startTime().toLocalDate();
        var freeSlots = slotEngine.generateFreeSlots(req.professionalId(), date, date, null);
        var slotAvailable = freeSlots.stream().anyMatch(dt -> dt.equals(req.startTime()));
        if (!slotAvailable) throw new SlotConflictException("Slot not available for this professional");

        var profConflicts = appointmentRepo.findConflictingByProfessional(
                req.professionalId(), req.startTime(), req.endTime());
        if (!profConflicts.isEmpty())
            throw new SlotConflictException("Professional already has an appointment in this time range");

        if (req.roomId() != null) {
            roomRepo.findById(req.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            var roomConflicts = appointmentRepo.findConflictingByRoom(
                    req.roomId(), req.startTime(), req.endTime());
            if (!roomConflicts.isEmpty())
                throw new SlotConflictException("Room is already booked for this time range");
        }

        var appointment = Appointment.builder()
                .clinicId(professional.getClinicId())
                .patientId(req.patientId())
                .professionalId(req.professionalId())
                .roomId(req.roomId())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status(Appointment.AppointmentStatus.SCHEDULED)
                .notes(req.notes())
                .build();
        appointment = appointmentRepo.save(appointment);

        var existingSlots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                req.professionalId(), req.startTime(), req.endTime());
        if (existingSlots.isEmpty()) {
            slotRepo.save(Slot.builder()
                    .professionalId(req.professionalId())
                    .startTime(req.startTime())
                    .endTime(req.endTime())
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
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getStatus().name(), LocalDateTime.now()));

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID id, Appointment.AppointmentStatus target, String reason) {
        var appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        var current = appointment.getStatus();
        var allowed = TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target))
            throw new InvalidTransitionException("Cannot transition from " + current + " to " + target);

        appointment.setStatus(target);
        appointment = appointmentRepo.save(appointment);

        if (target == Appointment.AppointmentStatus.CANCELLED) {
            var slots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                    appointment.getProfessionalId(), appointment.getStartTime(), appointment.getEndTime());
            for (var s : slots) {
                s.setStatus(Slot.SlotStatus.FREE);
                s.setPatientId(null);
                s.setRoomId(null);
            }
            slotRepo.saveAll(slots);
            cancellationLogService.log(appointment.getId(), appointment.getPatientId(), reason, "SYSTEM");
        }

        var eventType = switch (target) {
            case CONFIRMED -> "confirmed";
            case CANCELLED -> "cancelled";
            default -> "updated";
        };
        eventProducer.publish(new AppointmentEvent(eventType, appointment.getId(),
                appointment.getClinicId(), appointment.getPatientId(),
                appointment.getProfessionalId(), appointment.getRoomId(),
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getStatus().name(), LocalDateTime.now()));

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse createSession(UUID id, String prontuario) {
        var appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (appointment.getStatus() != Appointment.AppointmentStatus.CONFIRMED)
            throw new InvalidTransitionException("Only CONFIRMED appointments can create sessions (current: " + appointment.getStatus() + ")");

        appointment.setStatus(Appointment.AppointmentStatus.DONE);
        if (prontuario != null) appointment.setProntuario(prontuario);
        appointment = appointmentRepo.save(appointment);

        sessionEventProducer.publish(new SessionEvent(appointment.getId(),
                appointment.getClinicId(), appointment.getPatientId(),
                appointment.getProfessionalId(),
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getNotes(), appointment.getProntuario(), LocalDateTime.now()));

        return toResponse(appointment);
    }

    public AppointmentResponse findById(UUID id) {
        return appointmentRepo.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    @Transactional
    public void delete(UUID id) {
        var appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (appointment.getStatus() != Appointment.AppointmentStatus.SCHEDULED)
            throw new InvalidTransitionException("Only SCHEDULED appointments can be deleted (current: " + appointment.getStatus() + ")");

        var slots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                appointment.getProfessionalId(), appointment.getStartTime(), appointment.getEndTime());
        for (var s : slots) {
            s.setStatus(Slot.SlotStatus.FREE);
            s.setPatientId(null);
            s.setRoomId(null);
        }
        slotRepo.saveAll(slots);

        cancellationLogService.log(appointment.getId(), appointment.getPatientId(), "Appointment deleted", "SYSTEM");
        appointmentRepo.delete(appointment);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getClinicId(), a.getPatientId(),
                a.getProfessionalId(), a.getRoomId(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getStatus().name(), a.getProntuario(), a.getNotes(),
                a.getCreatedAt().toString(), a.getUpdatedAt().toString());
    }
}
