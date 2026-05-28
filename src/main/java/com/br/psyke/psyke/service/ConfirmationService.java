package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.ConfirmationResponse;
import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.ConfirmationToken;
import com.br.psyke.psyke.repository.AppointmentRepository;
import com.br.psyke.psyke.repository.ConfirmationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmationService {

    private final ConfirmationTokenRepository tokenRepo;
    private final AppointmentRepository appointmentRepo;
    private final AppointmentEventProducer eventProducer;

    public ConfirmationToken generate(UUID appointmentId, String type) {
        var appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        var token = ConfirmationToken.builder()
                .token(UUID.randomUUID())
                .appointmentId(appointmentId)
                .type(type)
                .expiresAt(OffsetDateTime.now().plusHours(48))
                .build();
        return tokenRepo.save(token);
    }

    @Transactional
    public ConfirmationResponse confirm(UUID token) {
        var ct = tokenRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Confirmation token not found"));

        if (!ct.isValid()) return new ConfirmationResponse("EXPIRED", ct.getAppointmentId(), ct.getToken());

        var appointment = appointmentRepo.findById(ct.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        ct.setUsedAt(OffsetDateTime.now());
        appointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);

        eventProducer.publish(new AppointmentEvent("confirmed", appointment.getId(),
                appointment.getClinicId(), appointment.getPatientId(),
                appointment.getProfessionalId(), appointment.getRoomId(),
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getStatus().name(), LocalDateTime.now()));

        return new ConfirmationResponse("CONFIRMED", ct.getAppointmentId(), ct.getToken());
    }

    @Transactional
    public ConfirmationResponse cancel(UUID token) {
        var ct = tokenRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Confirmation token not found"));

        if (!ct.isValid()) return new ConfirmationResponse("EXPIRED", ct.getAppointmentId(), ct.getToken());

        var appointment = appointmentRepo.findById(ct.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        ct.setUsedAt(OffsetDateTime.now());
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

        eventProducer.publish(new AppointmentEvent("cancelled", appointment.getId(),
                appointment.getClinicId(), appointment.getPatientId(),
                appointment.getProfessionalId(), appointment.getRoomId(),
                appointment.getStartTime().toString(), appointment.getEndTime().toString(),
                appointment.getStatus().name(), LocalDateTime.now()));

        return new ConfirmationResponse("CANCELLED", ct.getAppointmentId(), ct.getToken());
    }
}
