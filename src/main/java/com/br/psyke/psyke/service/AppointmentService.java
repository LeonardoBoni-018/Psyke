package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.AppointmentRequest;
import com.br.psyke.psyke.dto.AppointmentResponse;
import com.br.psyke.psyke.dto.CalendarEvent;
import com.br.psyke.psyke.dto.UpdateAppointmentRequest;
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
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.repository.ProfessionalRepository;
import com.br.psyke.psyke.repository.RoomRepository;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    /** Matches the web app palette (dark theme). */
    private static final Map<Appointment.AppointmentStatus, String> CALENDAR_COLORS = Map.of(
        Appointment.AppointmentStatus.SCHEDULED, "#f5a623",
        Appointment.AppointmentStatus.CONFIRMED, "#0ec4a0",
        Appointment.AppointmentStatus.CANCELLED, "#4a6478",
        Appointment.AppointmentStatus.NO_SHOW, "#e05555",
        Appointment.AppointmentStatus.DONE, "#8fa8b8"
    );

    private final AppointmentRepository appointmentRepo;
    private final SlotRepository slotRepo;
    private final ProfessionalRepository professionalRepo;
    private final PatientRepository patientRepo;
    private final RoomRepository roomRepo;
    private final AppointmentEventProducer eventProducer;
    private final SessionEventProducer sessionEventProducer;
    private final CancellationLogService cancellationLogService;
    private final SlotEngine slotEngine;

    public List<AppointmentResponse> findAll(UUID professionalId, UUID patientId, Appointment.AppointmentStatus status, LocalDateTime from, LocalDateTime to) {
        // Unbounded listing would load the whole table (OWASP API4): require a patient or a bounded period.
        if (patientId == null && (from == null || to == null))
            throw new IllegalArgumentException("Informe patientId ou o período (from e to)");
        if (patientId == null && from.plusDays(366).isBefore(to))
            throw new IllegalArgumentException("Período máximo de 366 dias");
        return toResponses(appointmentRepo.findAllByFilters(professionalId, patientId, status, from, to));
    }

    public List<CalendarEvent> findCalendarEvents(UUID professionalId, LocalDateTime from, LocalDateTime to) {
        if (from.plusDays(62).isBefore(to))
            throw new IllegalArgumentException("Período máximo de 62 dias no calendário");
        var clinicId = TenantContext.getClinicId() != null && !TenantContext.getClinicId().isBlank()
                ? UUID.fromString(TenantContext.getClinicId()) : null;

        var appointments = (professionalId != null
            ? appointmentRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(professionalId, from, to)
            : appointmentRepo.findAllByFilters(null, null, null, from, to))
            .stream().filter(a -> clinicId == null || clinicId.equals(a.getClinicId())).toList();
        var names = loadNames(appointments);

        return appointments.stream().map(a -> {
                var color = CALENDAR_COLORS.getOrDefault(a.getStatus(), "#4a9eff");
                var patientName = names.patients().getOrDefault(a.getPatientId(), "Paciente");
                var props = new java.util.HashMap<String, Object>();
                props.put("status", a.getStatus().name());
                props.put("patientId", a.getPatientId().toString());
                props.put("patientName", patientName);
                props.put("professionalId", a.getProfessionalId().toString());
                props.put("professionalName", names.professionals().getOrDefault(a.getProfessionalId(), ""));
                props.put("roomId", a.getRoomId() != null ? a.getRoomId().toString() : "");
                props.put("notes", a.getNotes() != null ? a.getNotes() : "");
                return new CalendarEvent(a.getId(), patientName,
                    a.getStartTime().toString(), a.getEndTime().toString(),
                    color, color, "#080c0f", props);
            }).toList();
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest req) {
        var professional = professionalRepo.findById(req.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
        patientRepo.findById(req.patientId())
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

        return toDetailedResponse(appointment);
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
        if (target == Appointment.AppointmentStatus.DONE) recordPatientVisit(appointment);

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

        return toDetailedResponse(appointment);
    }

    /** Keeps the patient's first/last appointment dates in sync when a session is completed. */
    private void recordPatientVisit(Appointment appointment) {
        var visit = appointment.getStartTime().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        patientRepo.findById(appointment.getPatientId()).ifPresent(patient -> {
            OffsetDateTime first = patient.getFirstAppointment();
            OffsetDateTime last = patient.getLastAppointment();
            if (first == null || visit.isBefore(first)) patient.setFirstAppointment(visit);
            if (last == null || visit.isAfter(last)) patient.setLastAppointment(visit);
            patientRepo.save(patient);
        });
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
        recordPatientVisit(appointment);

        sessionEventProducer.publish(new SessionEvent(appointment.getId(),
                appointment.getClinicId(), appointment.getPatientId(),
                appointment.getProfessionalId(),
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getNotes(), appointment.getProntuario(), LocalDateTime.now()));

        return toDetailedResponse(appointment);
    }

    public AppointmentResponse findById(UUID id) {
        return appointmentRepo.findById(id).map(this::toDetailedResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    @Transactional
    public AppointmentResponse update(UUID id, UpdateAppointmentRequest req) {
        var appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (req.roomId() != null) {
            roomRepo.findById(req.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            appointment.setRoomId(req.roomId());
        }
        if (req.notes() != null) appointment.setNotes(req.notes());

        appointment = appointmentRepo.save(appointment);
        return toDetailedResponse(appointment);
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

    private record Names(Map<UUID, String> patients, Map<UUID, String> professionals, Map<UUID, String> rooms) {}

    /** Batch-loads display names: three queries per page instead of three per row (no N+1). */
    private Names loadNames(List<Appointment> appointments) {
        if (appointments.isEmpty()) return new Names(Map.of(), Map.of(), Map.of());
        var patientIds = appointments.stream().map(Appointment::getPatientId).collect(java.util.stream.Collectors.toSet());
        var professionalIds = appointments.stream().map(Appointment::getProfessionalId).collect(java.util.stream.Collectors.toSet());
        var roomIds = appointments.stream().map(Appointment::getRoomId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        var patients = new java.util.HashMap<UUID, String>();
        patientRepo.findAllById(patientIds).forEach(p -> patients.put(p.getId(), p.getFullName()));
        var professionals = new java.util.HashMap<UUID, String>();
        professionalRepo.findAllById(professionalIds).forEach(p -> professionals.put(p.getId(), p.getFullName()));
        var rooms = new java.util.HashMap<UUID, String>();
        if (!roomIds.isEmpty()) roomRepo.findAllById(roomIds).forEach(r -> rooms.put(r.getId(), r.getName()));
        return new Names(patients, professionals, rooms);
    }

    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        var names = loadNames(appointments);
        return appointments.stream().map(a -> withNames(a, names)).toList();
    }

    private AppointmentResponse toDetailedResponse(Appointment a) {
        return withNames(a, loadNames(List.of(a)));
    }

    private AppointmentResponse withNames(Appointment a, Names names) {
        return new AppointmentResponse(a.getId(), a.getClinicId(), a.getPatientId(),
                a.getProfessionalId(), a.getRoomId(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getStatus().name(), a.getProntuario(), a.getNotes(),
                a.getCreatedAt().toString(), a.getUpdatedAt().toString(),
                names.patients().get(a.getPatientId()),
                names.professionals().get(a.getProfessionalId()),
                a.getRoomId() != null ? names.rooms().get(a.getRoomId()) : null);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getClinicId(), a.getPatientId(),
                a.getProfessionalId(), a.getRoomId(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getStatus().name(), a.getProntuario(), a.getNotes(),
                a.getCreatedAt().toString(), a.getUpdatedAt().toString());
    }
}
