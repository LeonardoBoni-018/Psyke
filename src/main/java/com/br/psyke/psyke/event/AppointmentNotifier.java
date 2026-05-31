package com.br.psyke.psyke.event;

import com.br.psyke.psyke.model.Patient;
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.service.EmailService;
import com.br.psyke.psyke.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentNotifier {

    private final PatientRepository patients;
    private final EmailService emailService;
    private final SmsService smsService;

    @KafkaListener(topics = "appointment.scheduled")
    public void onScheduled(AppointmentEvent event) {
        notifyPatient(event, "agendada");
    }

    @KafkaListener(topics = "appointment.confirmed")
    public void onConfirmed(AppointmentEvent event) {
        notifyPatient(event, "confirmada");
    }

    @KafkaListener(topics = "appointment.cancelled")
    public void onCancelled(AppointmentEvent event) {
        notifyPatient(event, "cancelada");
    }

    private void notifyPatient(AppointmentEvent event, String status) {
        var patient = patients.findById(event.patientId()).orElse(null);
        if (patient == null) {
            log.warn("Patient {} not found for notification", event.patientId());
            return;
        }
        var msg = "Sua consulta foi %s para %s.".formatted(status, event.startTime());
        if (patient.getPhone() != null) {
            smsService.send(patient.getPhone(), msg);
        }
        if (patient.getEmail() != null) {
            emailService.send(patient.getEmail(), "Atualização de Agendamento - Psyke", msg);
        }
    }
}
