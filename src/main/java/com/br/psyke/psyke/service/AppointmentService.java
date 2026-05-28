package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.AppointmentRequest;
import com.br.psyke.psyke.dto.AppointmentResponse;
import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.AppointmentEventProducer;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final SlotRepository slotRepo;
    private final UserRepository userRepo;
    private final RoomRepository roomRepo;
    private final AppointmentEventProducer eventProducer;
    private final CancellationLogService cancellationLogService;

    @Transactional
    public AppointmentResponse create(AppointmentRequest req) {
        var professional = userRepo.findById(req.professionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
        userRepo.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        var slotFree = slotRepo.existsByProfessionalIdAndStartTimeAndStatus(
                req.professionalId(), req.startTime(), Slot.SlotStatus.FREE);
        if (!slotFree) throw new SlotConflictException("Slot not available for this professional");

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

        var slots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                req.professionalId(), req.startTime(), req.endTime());
        for (var s : slots) {
            s.setStatus(Slot.SlotStatus.BOOKED);
            s.setPatientId(req.patientId());
            s.setRoomId(req.roomId());
        }
        slotRepo.saveAll(slots);

        eventProducer.publish(new AppointmentEvent("scheduled", appointment.getId(),
                appointment.getClinicId(), appointment.getPatientId(),
                appointment.getProfessionalId(), appointment.getRoomId(),
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getStatus().name(), LocalDateTime.now()));

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID id, Appointment.AppointmentStatus status, String reason) {
        var appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        appointment.setStatus(status);
        appointment = appointmentRepo.save(appointment);

        if (status == Appointment.AppointmentStatus.CANCELLED) {
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

        var eventType = switch (status) {
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

    public AppointmentResponse findById(UUID id) {
        return appointmentRepo.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getClinicId(), a.getPatientId(),
                a.getProfessionalId(), a.getRoomId(),
                a.getStartTime().toString(), a.getEndTime().toString(),
                a.getStatus().name(), a.getProntuario(), a.getNotes(),
                a.getCreatedAt().toString(), a.getUpdatedAt().toString());
    }
}
