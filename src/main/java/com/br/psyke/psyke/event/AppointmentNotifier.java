package com.br.psyke.psyke.event;

import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.service.EmailService;
import com.br.psyke.psyke.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Sends SMS/e-mail to the patient when an appointment changes. Invoked with the tenant already bound. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentNotifier {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private final PatientRepository patients;
    private final EmailService emailService;
    private final SmsService smsService;

    public void handle(AppointmentEvent event) {
        switch (event.eventType()) {
            case "scheduled" -> notifyPatient(event, "agendada");
            case "confirmed" -> notifyPatient(event, "confirmada");
            case "cancelled" -> notifyPatient(event, "cancelada");
            default -> { /* no patient notification */ }
        }
    }

    private void notifyPatient(AppointmentEvent event, String status) {
        var patient = patients.findById(event.patientId()).orElse(null);
        if (patient == null) {
            log.warn("Patient {} not found for notification", event.patientId());
            return;
        }
        var msg = "Sua consulta foi %s para %s.".formatted(status, format(event.startTime()));
        if (patient.getPhone() != null && !patient.getPhone().isBlank()) {
            smsService.send(patient.getPhone(), msg);
        }
        if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
            emailService.send(patient.getEmail(), "Atualização de agendamento - Psyke", msg);
        }
    }

    private static String format(String isoDateTime) {
        try {
            return LocalDateTime.parse(isoDateTime).format(WHEN);
        } catch (RuntimeException e) {
            return isoDateTime;
        }
    }
}
