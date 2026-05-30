package com.br.psyke.psyke.event;

import com.br.psyke.psyke.model.EvolucaoClinica;
import com.br.psyke.psyke.model.Prontuario;
import com.br.psyke.psyke.repository.EvolucaoClinicaRepository;
import com.br.psyke.psyke.repository.ProntuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    private final ProntuarioRepository prontuarioRepo;
    private final EvolucaoClinicaRepository evolucaoRepo;

    @KafkaListener(topics = "appointment.session.created")
    @Transactional
    public void onSessionCreated(SessionEvent event) {
        log.info("Received session event for appointment {}", event.appointmentId());

        var prontuario = prontuarioRepo.findByPatientId(event.patientId()).orElse(null);
        if (prontuario == null) {
            log.warn("No prontuário found for patient {}, skipping auto-evolution", event.patientId());
            return;
        }

        if (evolucaoRepo.existsByAppointmentId(event.appointmentId())) {
            log.debug("Evolution already exists for appointment {}", event.appointmentId());
            return;
        }

        var evolucao = EvolucaoClinica.builder()
                .prontuarioId(prontuario.getId())
                .appointmentId(event.appointmentId())
                .professionalId(event.professionalId())
                .objective(event.prontuario())
                .build();

        evolucaoRepo.save(evolucao);
        log.info("Auto-created clinical evolution from session {}", event.appointmentId());
    }
}
